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

	// Returns true if next character is not part of a whitespace
	private boolean isWhiteSpace(){
		return cc == ' ' || cc == '\n' || cc == '\r' || cc == '\t';
	}
	
	private boolean isCommentStart(){
		return cc == '#';
	}
	
	private void skipWhiteSpace(){
		// Skip whitespace
		while(cc == '\n' || cc == '\r' || cc == ' ' || cc == '\t') {
			if(cc == '\n') {
				line++;
				character = 1;
			}
			cc = readNextCharacter();
		}
	}
	
	// Returns true if the next character is not part of a comment
	private Symbol skipComment(){
		// Remove comments starting with # and until end of line
		while (cc == '#') {
			while(cc != -1 && cc != '\n'){ // Continue until linebreak or eof
				cc = readNextCharacter();
			}
			if (cc == -1){
				return createSymbol(Symbol.Term.EOF, "");
			}
			if(cc == '\n'){
				cc = readNextCharacter();
				line++;	
			}
		}
		return null;
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

		while(isWhiteSpace() || isCommentStart()){
			Symbol eof = skipComment();
			if(eof != null)
				return eof;
			skipWhiteSpace();
		}

		String lexeme = (char)cc + "";
				System.out.println((char)cc);
		switch(cc) {
			case '!': // ! !=
				cc = readNextCharacter();
				if(cc == '=') {
					lexeme += (char)cc;					
					cc = readNextCharacter();
					return createSymbol(Symbol.Term.NEQ, lexeme);
				} else {
					return createSymbol(Symbol.Term.NOT, lexeme);
				}
			case '|': // |
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.IOR, lexeme); // TODO: CHECK IF CORRECT
			case '^': // ^
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.XOR, lexeme);
			case '&': // &
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.AND, lexeme);
			case '=': // ==, =
				cc = readNextCharacter();
				if(cc == '=') {
					lexeme += (char) cc;					
					cc = readNextCharacter();
					return createSymbol(Symbol.Term.EQU, lexeme);
				} else {
					return createSymbol(Symbol.Term.ASSIGN, lexeme);
				}
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
			case '+': // +
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.ADD, lexeme);
			case '-': // -
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.SUB, lexeme);
			case '*': // *
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.MUL, lexeme);
			case '/': // /
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.DIV, lexeme);
			case '%': // /
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.MOD, lexeme);
			case '$': // $
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.ADDR, lexeme); // TODO: CHECK IF CORRECT
			case '@': // @
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.ADDR, lexeme); // TODO: CHECK IF CORRECT
			case '.': // .
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.DOT, lexeme);
			case ',': // ,
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.COMMA, lexeme);
			case ':': // ,
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.COLON, lexeme);
			case ';': // ,
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.SEMIC, lexeme);
			case '[': // [
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.LBRACKET, lexeme);
			case ']': // ]
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.RBRACKET, lexeme);
			case '(': // (
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.LPARENTHESIS, lexeme);
			case ')': // )
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.RPARENTHESIS, lexeme);
			case '{': // {
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.LBRACE, lexeme);
			case '}': // }
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.RBRACE, lexeme);
				
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
