package compiler.phases.ralloc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import compiler.Main;
import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.AsmOPER;
import compiler.data.asmcode.Code;
import compiler.data.layout.Temp;
import compiler.phases.livean.LiveAn;

class Node{
	int reg;
	Temp temp;
	HashSet<Node> connections = new HashSet<>(); // Used for selection phase
	HashSet<Node> neighbours = new HashSet<>(); // Used for coloring
	
	public Node(Temp t) {
		this.temp = t;
		
		reg = -1;
	}
}

public class RAllocCode {

	private RAlloc rAlloc;
	private Code code;
	
	private HashMap<Temp, Node> nodes;
	private HashMap<Temp, Integer> regs;
	
	private Stack<Node> toColor = new Stack<>();
	private int tempSize = 0;
	
	public RAllocCode(RAlloc rAlloc, Code code) {
		this.code = code;
		this.rAlloc = rAlloc;
	}
	
	public void run() {
		boolean succeeded = false;
		while(!succeeded) {
			build();
			simplify();
			succeeded = tryColorOrFix();
			
			if(!succeeded) {
				LiveAn liveAn = new LiveAn();
				liveAn.chunkLiveness(code);
				liveAn.log();
			}
		}
		
		Code newCode = new Code(code.frame, code.entryLabel, code.exitLabel, code.instrs, regs, tempSize);
		rAlloc.newCodes.add(newCode);
		
	}
	
	private void build() {
		// Return temp can be whatever TODO
		regs = new HashMap<Temp, Integer>();
		regs.put(code.frame.RV, 1);
		
		nodes = new HashMap<Temp, Node>();
		// Add all temps to graph
		for(AsmInstr inst: code.instrs) {
			for(Temp t: inst.in()) {
				if(!nodes.containsKey(t)) {
					nodes.put(t, new Node(t));
				}
			}
			for(Temp t: inst.out()) {
				if(!nodes.containsKey(t)) {
					nodes.put(t, new Node(t));
				}
			}
			for(Temp t: inst.defs()) {
				if(!nodes.containsKey(t)) {
					nodes.put(t, new Node(t));
				}
			}
		}
		// Insert connections
		for(AsmInstr inst: code.instrs) {
			// Two in the same in
			for(Temp t1 :inst.in()) {
				for(Temp t2 :inst.in()) {
					if(t1 != t2) {
						nodes.get(t1).connections.add(nodes.get(t2));
						nodes.get(t2).connections.add(nodes.get(t1));
						
						nodes.get(t1).neighbours.add(nodes.get(t2));
						nodes.get(t2).neighbours.add(nodes.get(t1));
					}
				}	
			}
			
			// Two in the same out
			for(Temp t1 :inst.out()) {
				for(Temp t2 :inst.out()) {
					if(t1 != t2) {
						nodes.get(t1).connections.add(nodes.get(t2));
						nodes.get(t2).connections.add(nodes.get(t1));
						
						nodes.get(t1).neighbours.add(nodes.get(t2));
						nodes.get(t2).neighbours.add(nodes.get(t1));
					}
				}	
			}
		}
	}
	
	// Returns true if it succeeded
	private void simplify() {
		Node toRemove = null;
		while(nodes.size() > 0 && (toRemove = nodeToRemove()) != null) {
			toColor.push(toRemove);
			
			// Remove the node from the graph
			for(Node n: nodes.values()) {
				n.connections.remove(toRemove);
			}
			nodes.remove(toRemove.temp);
		}
	}
	
	private Node nodeToRemove() {
		for(Node n: nodes.values()) {
			if(n.connections.size() < Main.numOfRegs) {
				return n;
			}
		}
		Node spill = spill();

		// If none was found return spill
		return spill;
	}
	
	private Node spill() {

		Vector<Node> spills = new Vector<>();
		for(Node n: nodes.values()) {
			if(spills.isEmpty() || n.connections.size() > spills.get(0).connections.size()) {
				spills.clear();
				spills.add(n);
			}
		}
		
		Node maxDeg = null;
		int maxD = 0;
		for(Node n: spills) {
			int start = Integer.MAX_VALUE;
			int end = 0;
			for(int i = 0; i < code.instrs.size();i++) {
				AsmInstr instr = code.instrs.get(i);
				if(instr.defs().contains(n.temp)) {
					start = Math.min(start, i);
				}
				if(instr.uses().contains(n.temp)) {
					end = Math.max(end, i);
				}
			}
			int d = end - start;
			if(d > maxD) {
				maxD = d;
				maxDeg = n;
			}

		}
		if(maxDeg == null)
			return spills.get(0);
		
		return maxDeg;
	}
	
	private boolean tryColorOrFix() {
		boolean succeeded = true;
		while(toColor.size() > 0) {
			Node current = toColor.pop();
			for(int color = 0; color < Main.numOfRegs; color++){
				boolean availableColor = true;
				for(Node neighbour : current.neighbours) {
					if(neighbour.reg == color) {
						availableColor = false;
					}
				}
				if(availableColor) {
					current.reg = color;
					regs.put(current.temp, color);
					break;
				}
			}
			if(current.reg == -1) {
				modifyCodeToSpill(current.temp);
				succeeded = false;
				break;
			}
		}
		return succeeded;
	}
	
	private void modifyCodeToSpill(Temp t) {
		tempSize += 8;
				
		Vector<AsmInstr> newInstrs = new Vector<>();
		
		for(AsmInstr instr : code.instrs) {
			boolean inUses = instr.uses().contains(t);
			boolean inDefs = instr.defs().contains(t);
			if(inDefs || inUses) {
				// Handle it properly
				long offset = code.frame.locsSize + 16 + tempSize;

				// Load address into a new temp
				Temp addressTemp = new Temp();
				Vector<Temp> defs, uses;				
				
				defs = new Vector<>();
				defs.addElement(addressTemp);
				newInstrs.add(new AsmOPER("SETL `d0," + offset, null, defs, null));

				uses = new Vector<>();
				uses.add(addressTemp);
				newInstrs.add(new AsmOPER("SUB `d0,$253,`s0", uses, defs, null)); // Harcoded FP
				
				Temp valueTemp = new Temp();
				
				// If the instruction uses the temp, load it
				if (inUses) {
					defs = new Vector<>();
					defs.add(valueTemp);
					uses = new Vector<>();
					uses.add(addressTemp);
					newInstrs.add(new AsmOPER("LDO `d0,`s0,0", uses, defs, null));
				}
				
				//newInstrs.add(instr);
				AsmOPER oper = (AsmOPER) instr;
				Vector<Temp> operUses = oper.uses();
				Vector<Temp> operDefs = oper.defs();
				Collections.replaceAll(operUses, t, valueTemp);
				Collections.replaceAll(operDefs, t, valueTemp);
				newInstrs.add(new AsmOPER(oper.instr(), uses, defs, oper.jumps()));
				
				// Save the temp if it was defined (as in, changed)
				if(inDefs) {					
					uses = new Vector<>();
					uses.add(valueTemp);
					uses.add(addressTemp);
					newInstrs.add(new AsmOPER("STO `s0,`s1,0", uses, null, null));
				}
				
			}else {
				newInstrs.add(instr);
			}
		}
		
		//System.out.println("Updating instructions, now with " + newInstrs.size() + " previous: " + code.instrs.size());
		code = new Code(code.frame, code.entryLabel, code.exitLabel, newInstrs);
	}
}
