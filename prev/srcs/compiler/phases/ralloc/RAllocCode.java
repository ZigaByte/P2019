package compiler.phases.ralloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import compiler.Main;
import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.Code;
import compiler.data.layout.Temp;
import compiler.phases.asmcode.AsmGen;

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
	
	public RAllocCode(RAlloc rAlloc, Code code) {
		this.code = code;
		this.rAlloc = rAlloc;
	}
	
	public void run() {
		build();
		
		boolean simplified = simplify();
		System.out.println(simplified);
		// Do the spill
		select();
		
		int tempSize = 0; // TODO
		Code newCode = new Code(code.frame, code.entryLabel, code.exitLabel, code.instrs, regs, tempSize);
		rAlloc.newCodes.add(newCode);
	}
	
	private void build() {
		// Return temp can be whatever
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
	private boolean simplify() {
		Node toRemove = null;
		while(nodes.size() > 0 && (toRemove = nodeToRemove()) != null) {
			toColor.push(toRemove);
			
			// Remove the node from the graph
			for(Node n: nodes.values()) {
				n.connections.remove(toRemove);
			}
			nodes.remove(toRemove.temp);
		}
		
		return nodes.size() == 0;
	}
	
	private Node nodeToRemove() {
		for(Node n: nodes.values()) {
			if(n.connections.size() < Main.numOfRegs) {
				return n;
			}
		}
		
		// If none was found return spill
		return spill();
	}
	
	private Node spill() {
		Node maxDeg = null;
		for(Node n: nodes.values()) {
			if(maxDeg == null || n.connections.size() > maxDeg.connections.size()) {
				maxDeg = n;
			}
		}
		System.out.println("Spill: " + maxDeg.temp);
		return maxDeg;
	}
	
	private void select() {
		System.out.println(toColor.size() + "asdfa ");
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
					System.out.println("Color found for " + current.temp + " " + color);
					break;
				}
			}
			if(current.reg == -1) {
				System.out.println("NO COLOR FOUND " +current.temp );
			}
		}
						
	}
			
			
}
