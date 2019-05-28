
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
		writer.println("\tLOC\t#100");
		writer.println("		GREG	@\r\n" + 
				"D254	OCTA 0\r\n" + 
				"		GREG	@\r\n" + 
				"D253	OCTA 0\r\n" + 
				"		GREG	@\r\n" + 
				"D252	OCTA 0\r\n" + 
				"		GREG	@\r\n" + 
				"D251	OCTA 0"); // Reserve some global registers
		
		writer.println("\n\n\tGREG @");
		writer.println("_print	OCTA 0,0,0,0,0,0,0,0,0,0,0\r\n" + 
				"		OCTA 0,0,0,0,0,0,0,0,0,0,0 % Print space");
		
		int size = 184;
		for(DataChunk chunk : Chunks.dataChunks) {
			if(size >= 256) {
				size = 0;
				writer.println("\tGREG @");
			}
			if(chunk.init != null && chunk.init.startsWith("\"")) {
				String decl = chunk.label.name + "\t\t" + "OCTA\t" + chunk.init + ",0";
				size += chunk.init.length()*8 + 1;
				writer.println(decl);
			} else {
				String decl = chunk.label.name + "\t" + "OCTA\t";
				size += chunk.size;
				for(int i = 0; i < chunk.size / 8; i++) {
					decl += i == 0 ? "0" : ",0";
				}
				writer.println(decl);
			}
		}
		
		writer.println();

		// Entry point
		writer.println();
		writer.println("% Entry point");
		writer.println("Main\tSET $0,#FFFF");
		
		// Set SP and FP initial value
		writer.println("\tSETH $251,#2000 % Heap Pointer");
		writer.println("\tSETH $252,#4000 % Stack Pointer"); // SP
		writer.println("\tINCL $252,#FFFF"); // SP
		writer.println("\tSETH $253,#0 % Frame Pointer"); // FP, should not matter, but still // TODO: cONSIDER THE FFFF
		
		// Jump to main?
		writer.println("\tPUSHJ $0,_main");
		
		writer.println("\tTRAP 0,Halt,0");
		
		// Functions
		writer.println();
		writer.println("% Functions");
		writer.println();

		for(Code code : ralloc.newCodes) {
			writer.println("% fun: " + code.frame.label.name);


			writer.println("% - Prologue");
			writer.println(code.frame.label.name + "\tSET $0,0 ");
			
			//System.out.println(code.frame.locsSize);
			//System.out.println(code.frame.argsSize);
			//System.out.println(code.frame.size);
			
			// Save the old FP
			writer.println("\tSET $0,$252");
			writer.println("\tSETL $2,"+ (code.frame.locsSize + 8));
			writer.println("\tSUB $0,$0,$2" );
			writer.println("\tSTO $253,$0,0");
			
			// Save the return address
			writer.println("\tSUB $0,$0," + 8);
			writer.println("\tGET $1,rJ");
			writer.println("\tSTO $1,$0,0");
			
			// Increase FP and SP
			writer.println("\tSET $253,$252");
			writer.println("\tSETL $2,"+ code.frame.size);
			writer.println("\tSUB $252,$252,$2");
			
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
			writer.println("\tSETL $2,"+ (code.frame.locsSize + 8));
			writer.println("\tSUB $0,$0,$2");
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
		writer.println("_del	POP\r\n" + 
				"_new	STO $251,$252,0\r\n" + 
				"	SET $0,$252\r\n" + 
				"	ADD $0,$0,8\r\n" + 
				"	LDO $1,$0,0\r\n" + 
				"	ADD $251,$251,$1\r\n" + 
				"	POP");
		writer.println("_putInt	SET $0,$252\r\n" + 
				"	ADD $0,$0,8\r\n" + 
				"	LDO $0,$0,0 % value in $0\r\n" + 
				"	CMP $4,$0,0\r\n" + 
				"	CSN $3,$4,1 % $3 = 1 if we have a negative number\r\n" + 
				"	BZ $3,PI0\r\n" + 
				"	NEG $0,$0 % Negate if negative\r\n" + 
				"	SET $2,0 % Cell count in $2\r\n" + 
				"PI0	DIV $0,$0,10\r\n" + 
				"	GET	$1,rR\r\n" + 
				"	ADD $1,$1,48 $ Current digit in $1\r\n" + 
				"	LDA $4,_print\r\n" + 
				"	STO $1,$4,$2\r\n" + 
				"	ADD $2,$2,8\r\n" + 
				"	BNZ $0,PI0\r\n" + 
				"	BZ $3,PI2\r\n" + 
				"	SET $1,45 % Get - if negative\r\n" + 
				"	STO $1,$4,$2\r\n" + 
				"	ADD $2,$2,8\r\n" + 
				"PI2	SUB $2,$2,8\r\n" + 
				"	LDA $255,_print\r\n" + 
				"	ADD $255,$255,7\r\n" + 
				"	ADD $255,$255,$2\r\n" + 
				"	TRAP 0,Fputs,StdOut\r\n" + 
				"	CMP $4,$2,0\r\n" + 
				"	BNZ $4,PI2\r\n" + 
				"PI1	POP");
		writer.println("_putChar	SET $0,$252\r\n" + 
				"	ADD $0,$0,8\r\n" + 
				"	LDO $0,$0,0\r\n" + 
				"	LDA $255,_print\r\n" + 
				"	STO $0,$255,0\r\n" + 
				"	ADD $255,$255,7\r\n" + 
				"	TRAP 0,Fputs,StdOut\r\n" + 
				"	POP");
		writer.println("_putString SET $0,$252\r\n" + 
				"	ADD $0,$0,8\r\n" + 
				"	LDO $0,$0,0\r\n" + 
				"	SET $255,$0\r\n" + 
				"	SET $2,7\r\n" + 
				"	ADD $255,$255,$2\r\n" + 
				"PS0	TRAP 0,Fputs,StdOut\r\n" + 
				"	ADD $2,$2,8\r\n" + 
				"	ADD $255,$0,$2\r\n" + 
				"	LDO $1,$255,0\r\n" + 
				"	BNZ $1,PS0\r\n" + 
				"	POP");

		writer.close();
	}
	
}
