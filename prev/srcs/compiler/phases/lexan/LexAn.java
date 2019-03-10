/**
 * @author sliva
 */
package compiler.phases.lexan;

import java.io.*;
import java.util.HashMap;

import javafx.scene.input.KeyCode;
import compiler.common.report.*;
import compiler.common.report.Report.Error;
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
	
	private final HashMap<String, Symbol.Term> keywords = new HashMap<String, Symbol.Term>();

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
		
		keywords.put("arr", Symbol.Term.ARR);
		keywords.put("bool", Symbol.Term.BOOL);
		keywords.put("char", Symbol.Term.CHAR);
		keywords.put("del", Symbol.Term.DEL);
		keywords.put("do", Symbol.Term.DO);
		keywords.put("else", Symbol.Term.ELSE);
		keywords.put("end", Symbol.Term.END);
		keywords.put("fun", Symbol.Term.FUN);
		keywords.put("if", Symbol.Term.IF);
		keywords.put("int", Symbol.Term.INT);
		keywords.put("new", Symbol.Term.NEW);
		keywords.put("ptr", Symbol.Term.PTR);
		keywords.put("rec", Symbol.Term.REC);
		keywords.put("then", Symbol.Term.THEN);
		keywords.put("typ", Symbol.Term.TYP);
		keywords.put("var", Symbol.Term.VAR);
		keywords.put("void", Symbol.Term.VOID);
		keywords.put("where", Symbol.Term.WHERE);
		keywords.put("while", Symbol.Term.WHILE);
		
		keywords.put("none", Symbol.Term.VOIDCONST);
		keywords.put("true", Symbol.Term.BOOLCONST);
		keywords.put("false", Symbol.Term.BOOLCONST);
		keywords.put("null", Symbol.Term.PTRCONST);
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
		}
		//System.out.println(String.format("Returning %s", symb));
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
			if(cc == ' '){
				character++;
			}else if(cc == '\t'){
				character += 8; // Tab is 8 spaces 
			}
			if(cc == '\n') {
				nextLine();
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
				nextLine();
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
				return createSymbol(Symbol.Term.IOR, lexeme);
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
				return createSymbol(Symbol.Term.ADDR, lexeme); 
			case '@': // @
				cc = readNextCharacter();
				return createSymbol(Symbol.Term.DATA, lexeme);
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
				
			default: // All other things, literals, keywords, identifiers
				if ('0' <= cc && cc <= '9') { // literal of type int
					cc = readNextCharacter();
					while('0' <= cc && cc <= '9'){
						lexeme += (char)cc;
						cc = readNextCharacter();
					}
					return createSymbol(Symbol.Term.INTCONST, lexeme);
				}
				if (cc == '\'') { // literal of type char
					cc = readNextCharacter();
					if(32 <= cc && cc <= 126){
						lexeme += (char) cc;
						cc = readNextCharacter();
						if(cc == '\''){
							lexeme += (char) cc;
							cc = readNextCharacter();
							return createSymbol(Symbol.Term.CHARCONST, lexeme);
						}else{
							Error noClosingError = createError("CHARCONST missing closing '.");
							throw noClosingError;
						}
					}else{
						Error characterNotSupportedError = createError("Character not supported");
						throw characterNotSupportedError;
					}
				}
				
				if(cc == '"'){ // literal of type string
					// Read content
					cc = readNextCharacter();
					while(32 <= cc && cc <= 126 && cc != '"'){
						lexeme += (char)cc;
						cc = readNextCharacter();
					}
					
					// Closing "
					if(cc == '"'){
						lexeme += (char) cc;
						cc = readNextCharacter();
						return createSymbol(Symbol.Term.STRCONST, lexeme);
					}else{
						Error noClosingError = createError("STRCONST missing closing \".");
						throw noClosingError;
					}
				}
				
				// keywords, literals (none, true, false, null), identifiers
				if(('A' <= cc && cc <= 'Z') || ('a' <= cc && cc <= 'z') || cc == '_'){
					cc = readNextCharacter();
					while(('A' <= cc && cc <= 'Z') || ('a' <= cc && cc <= 'z') || ('0' <= cc && cc <= '9') || cc == '_'){
						lexeme += (char)cc;
						cc = readNextCharacter();
					}
					
					// Check if keyword or literal
					if(keywords.containsKey(lexeme)){
						return createSymbol(keywords.get(lexeme), lexeme);
					}
					
					// Else identifier
					return createSymbol(Symbol.Term.IDENTIFIER, lexeme);
				}
				
				break;
		}
		
		Error noClosingError = createError("Unknown character.");
		throw noClosingError;
	}
	
	public void nextLine(){
		line++;
		character = 1;
	}
	
	int line = 1;
	int character = 1;
	int cc = 0; // Current character
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
	
	private Error createError(String message) {
		return new Error(new Location(line, character, line, character), message);
	}

}
