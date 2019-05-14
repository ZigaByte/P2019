/**
 * @author sliva
 */
package compiler.phases.livean;

import java.util.*;
import compiler.data.asmcode.*;
import compiler.data.layout.*;
import compiler.phases.*;
import compiler.phases.asmcode.*;

/**
 * @author sliva
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	public void chunkLiveness(Code code) {
		boolean changed = true;
		while(changed) {
			changed = false;
			for (int i = 0; i < code.instrs.size(); i++) {
				AsmInstr inst = code.instrs.get(i);
				{ // IN
					HashSet<Temp> toAdd = new HashSet<>();
					toAdd.addAll(inst.uses());
					
					HashSet<Temp> outs = new HashSet<>();
					outs.addAll(inst.out());
					outs.removeAll(inst.defs());
					
					toAdd.addAll(outs);
					
					/// Just get the new ones
					toAdd.removeAll(inst.in());
					
					if(toAdd.size() > 0) {
						changed = true;
						inst.addInTemps(toAdd);		
					}
				}
				
				{ // OUT
					HashSet<Temp> toAdd = new HashSet<>();
					
					// Add from next instruction, unless this is an unconditional jump?
					if(i != code.instrs.size() - 1) {
						toAdd.addAll(code.instrs.get(i+1).in());
					}
					
					// Add all jumps
					for(Label l : inst.jumps()) {
						// Find the instruction we can jump to.
						AsmInstr succ = null;
						for (AsmInstr instruction: code.instrs) {
							if(instruction instanceof AsmLABEL && ((AsmLABEL)instruction).getLabel().equals(l)) {
								succ = instruction;
							}
						}
						if(succ != null) {
							toAdd.addAll(succ.in());
						}
					}

					toAdd.removeAll(inst.out());
					if(toAdd.size() > 0) {
						changed = true;
						inst.addOutTemp(toAdd);			
					}
					
				}
			}
		}
	}

	public void chunksLiveness() {
		for (Code code : AsmGen.codes) {
			chunkLiveness(code);
		}
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			{
				logger.begElement("code");
				logger.addAttribute("entrylabel", code.entryLabel.name);
				logger.addAttribute("exitlabel", code.exitLabel.name);
				logger.addAttribute("tempsize", Long.toString(code.tempSize));
				code.frame.log(logger);
				logger.begElement("instructions");
				for (AsmInstr instr : code.instrs) {
					logger.begElement("instruction");
					logger.addAttribute("code", instr.toString());
					logger.begElement("temps");
					logger.addAttribute("name", "use");
					for (Temp temp : instr.uses()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "def");
					for (Temp temp : instr.defs()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "in");
					for (Temp temp : instr.in()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.begElement("temps");
					logger.addAttribute("name", "out");
					for (Temp temp : instr.out()) {
						logger.begElement("temp");
						logger.addAttribute("name", temp.toString());
						logger.endElement();
					}
					logger.endElement();
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
		}
	}

}
