package compiler.phases.wrapup;

import java.io.IOException;
import java.io.PrintWriter;

import compiler.Main;
import compiler.data.chunk.CodeChunk;
import compiler.data.chunk.DataChunk;
import compiler.phases.Phase;
import compiler.phases.chunks.Chunks;

public class Wrapup extends Phase{
	
	private PrintWriter writer;
	
	public Wrapup() {
		super("wrapup");
		try {	
			 writer = new PrintWriter(Main.cmdLineArgValue("--src-file-name").replace(".txt", ".mms"), "UTF-8");
		} catch (IOException e) {
			System.out.println("Could not write to file.");
		}
	} 
	
	public void print() throws IOException{
		writer.println("% " + Main.cmdLineArgValue("--src-file-name"));
		// Data segment
		writer.println("\t\tLOC\t\tData_Segment");
		writer.println("\t\tGREG\t@");
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
		writer.println("\t\tLOC\t\t#100");

		// Entry point
		writer.println();
		writer.println("% Entry point");
		writer.println("Main\tSET $0,15");
		
		// TODO: Startup
		
		writer.println("\t\tTRAP 0,Halt,0");
		
		// Functions
		writer.println();
		writer.println("% Functions");
		writer.println();

		for(CodeChunk chunk : Chunks.codeChunks) {
			writer.println("% fun: " + chunk.frame.label.name);

			writer.println();
			
			writer.println();
			writer.println();
		}
		
		// New, del, etc.

		writer.close();
	}
	
}
