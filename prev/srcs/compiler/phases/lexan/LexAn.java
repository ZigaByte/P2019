/**
 * @author sliva
 */
package compiler.phases.lexan;

import java.io.*;

import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.phases.*;

/**
 * Lexical analysis.
 * 
 * @author sliva
 */
public class LexAn extends Phase {

	/** The name of the source file. */
	private final String srcFileName;

	/** The source file reader. */
	private final BufferedReader srcFile;

	/**
	 * Constructs a new phase of lexical analysis.
	 */
	public LexAn() {
		super("lexan");
		srcFileName = compiler.Main.cmdLineArgValue("--src-file-name");
		try {
			srcFile = new BufferedReader(new FileReader(srcFileName));
		} catch (IOException ___) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException ___) {
			Report.warning("Cannot close source file '" + this.srcFileName + "'.");
		}
		super.close();
	}

	/**
	 * The lexer.
	 * 
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF. This method calls {@link #lexify()}, logs its result if
	 * requested, and returns it.
	 * 
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	public Symbol lexer() {
		Symbol symb = lexify();
		if (symb.token != Symbol.Term.EOF) {
			symb.log(logger);
			System.out.println("<"+symb.token + ", " + symb.lexeme + ", " + symb.location() +">");
		}
		return symb;
	}

	/**
	 * Performs the lexical analysis of the source file.
	 * 
	 * This method returns the next symbol from the source file. To perform the
	 * lexical analysis of the entire source file, this method must be called until
	 * it returns EOF.
	 * 
	 * @return The next symbol from the source file or EOF if no symbol is available
	 *         any more.
	 */
	private Symbol lexify() {
		if(cc == 0) { // Should only run the first time
			cc = readNextCharacter();
		}
		while(cc == '\n'|| cc == ' ') {
			if(cc == '\n') {
				line++;
				character = 1;
			}
			cc = readNextCharacter();
		}
		
		String lexeme = (char)cc + "";
				
		switch(cc) {
			
			case '+': // +
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.ADD, lexeme);
				
			case '-': // -
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.SUB, lexeme);
				
			case '<': // <, <=
				cc = readNextCharacter();
				if(cc == '=') {
					lexeme += (char)cc;					
					cc = readNextCharacter();
					return createSymbol(Symbol.Term.LEQ, lexeme);
				} else {
					return createSymbol(Symbol.Term.LTH, lexeme);
				}
			case '>': // >, >=
				cc = readNextCharacter();
				if(cc == '=') {
					lexeme += (char)cc;					
					cc = readNextCharacter();
					return createSymbol(Symbol.Term.GEQ, lexeme);
				} else {
					return createSymbol(Symbol.Term.GTH, lexeme);
				}
				
			case -1:
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.EOF, "");
		}
		
		return null;
	}
	
	int line = 1;
	int character = 1;
	int cc = 0;
	/**
	 * Reads the next character from the BufferedStream
	 * */
	// TODO: Eliminate whitespace
	private int readNextCharacter() {
		int readValue = 0;
		try {
			readValue = srcFile.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return readValue;
	} 
	
	private Symbol createSymbol(Symbol.Term term, String lexan) {
		int charStart = character;
		int charEnd = charStart + lexan.length() - 1;
		character += lexan.length();
		return new Symbol(term, lexan, new Location(line, charStart, line, charEnd));
	}

}
