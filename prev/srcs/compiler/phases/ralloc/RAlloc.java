/**
 * @author sliva
 */
package compiler.phases.ralloc;

import java.util.Vector;

import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.Code;
import compiler.phases.Phase;
import compiler.phases.asmcode.AsmGen;

/**
 * Register allocation phase.
 * 
 * @author sliva
 */
public class RAlloc extends Phase {

	public Vector<Code> newCodes = new Vector<Code>();
	
	public RAlloc() {
		super("ralloc");
	}

	/**
	 * Computes the mapping of temporary variables to registers for each function.
	 * If necessary, the code of each function is modified.
	 */
	public void tempsToRegs() {
		for(Code code : AsmGen.codes) {
			new RAllocCode(this, code).run();
		}
		AsmGen.codes = newCodes;
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("entrylabel", code.entryLabel.name);
			logger.addAttribute("exitlabel", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString(code.regs));
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}
