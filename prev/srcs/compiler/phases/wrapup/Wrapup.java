package compiler.phases.wrapup;

import java.io.IOException;
import java.io.PrintWriter;

import compiler.Main;
import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.AsmLABEL;
import compiler.data.asmcode.Code;
import compiler.data.chunk.DataChunk;
import compiler.phases.Phase;
import compiler.phases.chunks.Chunks;
import compiler.phases.ralloc.RAlloc;

public class Wrapup extends Phase{
	
	private PrintWriter writer;
	private RAlloc ralloc;
	
	public Wrapup(RAlloc ralloc) {
		super("wrapup");
		this.ralloc = ralloc;
		try {	
			 writer = new PrintWriter(Main.cmdLineArgValue("--src-file-name").replace(".txt", ".mms"), "UTF-8");
		} catch (IOException e) {
			System.out.println("Could not write to file.");
		}
	} 
	
	public void print() throws IOException{
		writer.println("% " + Main.cmdLineArgValue("--src-file-name"));
		// Data segment
		writer.println("\tLOC\t\tData_Segment");
		writer.println("\tGREG\t@");
		for(DataChunk chunk : Chunks.dataChunks) {
			if(chunk.init != null && chunk.init.startsWith("\"")) {
				String decl = chunk.label.name + "\t\t" + "BYTE\t" + chunk.init + ",0";
				writer.println(decl);
			} else {
				String decl = chunk.label.name + "\t" + "OCTA\t";
				for(int i = 0; i < chunk.size / 8; i++) {
					decl += i == 0 ? "0" : ",0";
				}
				writer.println(decl);
			}	
		}
		
		writer.println();
		writer.println("\tLOC\t\t#100");

		// Entry point
		writer.println();
		writer.println("% Entry point");
		writer.println("Main\tSET $0,#FFFF");
		// Make sure enough registers are global
		writer.println("\tSET $0,#FC");
		writer.println("\tPUT rG,$0");
		
		// Set SP and FP initial value
		writer.println("\tSETH $252,#6000"); // SP
		writer.println("\tINCL $252,#100"); // SP
		writer.println("\tSETH $253,#FFFF"); // FP, should not matter, but still // TODO: cONSIDER THE FFFF
		writer.println("\tPUT rJ,$0"); // rJ debug TODO: cONSIDER removing
		
		// Jump to main?
		writer.println("\tPUSHJ $0,_main");
		
		// TODO: Startup
		
		writer.println("\tTRAP 0,Halt,0");
		
		// Functions
		writer.println();
		writer.println("% Functions");
		writer.println();

		for(Code code : ralloc.newCodes) {
			writer.println("% fun: " + code.frame.label.name);


			writer.println("% - Prologue");
			writer.println(code.frame.label.name + "\tSET $0,0 ");
			
			// Save the old FP
			writer.println("\tSET $0,$252");
			writer.println("\tSUB $0,$0," + (code.frame.locsSize + 8));
			writer.println("\tSTO $253,$0,0");
			
			// Save the return address
			writer.println("\tSUB $0,$0," + 8);
			writer.println("\tGET $1,rJ");
			writer.println("\tSTO $1,$0,0");
			
			// Increase FP and SP
			writer.println("\tSET $253,$252");
			writer.println("\tSUB $252,$252," + code.frame.size);
			
			// Jump to body
			writer.println("\tJMP " + code.entryLabel.name);
					

			
			writer.println("% - Body");
			for(int i = 0; i < code.instrs.size(); i++) {
				AsmInstr instr = code.instrs.get(i);
				if(instr instanceof AsmLABEL) {
					AsmLABEL label = (AsmLABEL) instr;
					instr = code.instrs.get(i+1);
					writer.println(label.getLabel().name + "\t" + instr.toString(code.regs));
					i+=1;
				}else {
					writer.println("\t" + instr.toString(code.regs));
				}
			}
			
			
			writer.println("% - Epilogue");
			writer.println(code.exitLabel.name + "\tSTO $0,$253,0"); // Write return value to frame

			writer.println("\tSET $0,$253");
			writer.println("\tSUB $0,$0," + (code.frame.locsSize + 8));
			writer.println("\tLDO $1,$0,0"); // load old FP
			
			// Move SP and FP
			writer.println("\tSET $252,$253");
			writer.println("\tSET $253,$1");
			
			// Restore rJ
			writer.println("\tSUB $0,$0," + 8);
			writer.println("\tLDO $1,$0,0");
			writer.println("\tPUT rJ,$1");

			writer.println("\tPOP");
			
			writer.println();
			writer.println();
		}
		
		// New, del, etc.

		writer.close();
	}
	
}
