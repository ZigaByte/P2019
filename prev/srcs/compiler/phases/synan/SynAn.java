/**
 * @author sliva
 */
package compiler.phases.synan;

import compiler.common.report.*;
import compiler.data.symbol.*;
import compiler.data.symbol.Symbol.Term;
import compiler.data.dertree.*;
import compiler.data.dertree.DerNode.Nont;
import compiler.phases.*;
import compiler.phases.lexan.*;

/**
 * Syntax analysis.
 * 
 * @author sliva
 */
public class SynAn extends Phase {

	/** The derivation tree of the program being compiled. */
	public static DerTree derTree = null;

	/** The lexical analyzer used by this syntax analyzer. */
	private final LexAn lexAn;

	/**
	 * Constructs a new phase of syntax analysis.
	 */
	public SynAn() {
		super("synan");
		lexAn = new LexAn();
	}

	@Override
	public void close() {
		lexAn.close();
		super.close();
	}

	/**
	 * The parser.
	 * 
	 * This method constructs a derivation tree of the program in the source file.
	 * It calls method {@link #parseSource()} that starts a recursive descent parser
	 * implementation of an LL(1) parsing algorithm.
	 */
	public void parser() {
		currSymb = lexAn.lexer();
		derTree = parseSource();
		if (currSymb.token != Symbol.Term.EOF)
			throw new Report.Error(currSymb, "Unexpected '" + currSymb + "' at the end of a program.");
	}

	/** The lookahead buffer (of length 1). */
	private Symbol currSymb = null;

	/**
	 * Appends the current symbol in the lookahead buffer to a derivation tree node
	 * (typically the node of the derivation tree that is currently being expanded
	 * by the parser) and replaces the current symbol (just added) with the next
	 * input symbol.
	 * 
	 * @param node The node of the derivation tree currently being expanded by the
	 *             parser.
	 */
	private void add(DerNode node) {
		if (currSymb == null)
			throw new Report.InternalError();
		node.add(new DerLeaf(currSymb));
		currSymb = lexAn.lexer();
	}

	/**
	 * If the current symbol is the expected terminal, appends the current symbol in
	 * the lookahead buffer to a derivation tree node (typically the node of the
	 * derivation tree that is currently being expanded by the parser) and replaces
	 * the current symbol (just added) with the next input symbol. Otherwise,
	 * produces the error message.
	 * 
	 * @param node     The node of the derivation tree currently being expanded by
	 *                 the parser.
	 * @param token    The expected terminal.
	 * @param errorMsg The error message.
	 */
	private void add(DerNode node, Symbol.Term token, String errorMsg) {
		if (currSymb == null)
			throw new Report.InternalError();
		if (currSymb.token == token) {
			node.add(new DerLeaf(currSymb));
			currSymb = lexAn.lexer();
		} else
			throw new Report.Error(currSymb, errorMsg);
	}

	// source -> decls
	private DerNode parseSource() {
		DerNode node = new DerNode(DerNode.Nont.Source);
		node.add(parseDecls());
		return node;
	}

	// decls -> decl declsRest
	private DerNode parseDecls(){
		DerNode declsNode = new DerNode(DerNode.Nont.Decls);
		switch (currSymb.token) {
			case RBRACE:
			case EOF:
				return declsNode;
			default:		
				declsNode.add(parseDecl());
				declsNode.add(parseDecls());
				return declsNode;
		}

	}
	
	// decl -> typ identifier:type; 
	// decl -> var identifier:type;
	// decl -> identifier([identifier:type {,identifier:type}]):type [=expr];
	private DerNode parseDecl(){
		DerNode declNode = new DerNode(DerNode.Nont.Decl);
		switch(currSymb.token){
			case TYP:{
				add(declNode, Term.TYP, String.format("Expected symbol %s, but received %s.", Term.TYP, currSymb.token));
				add(declNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
				add(declNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
				declNode.add(parseType());
				add(declNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
				break;
			}
			case VAR:{
				add(declNode, Term.VAR, String.format("Expected symbol %s, but received %s.", Term.VAR, currSymb.token));
				add(declNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
				add(declNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
				declNode.add(parseType());
				add(declNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
				break;
			}
			case FUN:{
				add(declNode, Term.FUN, String.format("Expected symbol %s, but received %s.", Term.FUN, currSymb.token));
				add(declNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
				add(declNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
				declNode.add(parseArgs());
				add(declNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
				add(declNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
				declNode.add(parseType());
				declNode.add(parseBody());
				add(declNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
				
				break;
			}	
			default:
				throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
		return declNode;
	}
	
	private DerNode parseBody() {
		DerNode bodyNode = new DerNode(DerNode.Nont.BodyEps);
		switch (currSymb.token) {
		case SEMIC:
			return bodyNode;
		case ASSIGN:
			add(bodyNode, Term.ASSIGN, String.format("Expected symbol %s, but received %s.", Term.ASSIGN, currSymb.token));
			bodyNode.add(parseExpr());
			return bodyNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s not expected.", currSymb));
		}
	}
	
	private DerNode parseArg() {
		DerNode argNode = new DerNode(Nont.Arg);
		switch (currSymb.token) {
		case IDENTIFIER:
			add(argNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
			add(argNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
			argNode.add(parseType());
			return argNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseArgsRest() {
		DerNode argsNode = new DerNode(Nont.ArgsRest);
		switch (currSymb.token) {
		case COMMA:
			add(argsNode, Term.COMMA, String.format("Expected symbol %s, but received %s.", Term.COMMA, currSymb.token));
			argsNode.add(parseArg());
			argsNode.add(parseArgsRest());
			return argsNode;
		case RPARENTHESIS:
			return argsNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseArgs() {
		DerNode argsNode = new DerNode(Nont.Args);
		switch (currSymb.token) {
		case IDENTIFIER:
			argsNode.add(parseArg());
			argsNode.add(parseArgsRest());
			return argsNode;
		case RPARENTHESIS:
			return argsNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseType(){
		DerNode typeNode = new DerNode(Nont.Type);
		switch(currSymb.token){
			case IDENTIFIER:
				add(typeNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
				return typeNode;
			case LPARENTHESIS:{
				add(typeNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
				typeNode.add(parseType());
				add(typeNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
				return typeNode;
			}
			case VOID:
				add(typeNode, Term.VOID, String.format("Expected symbol %s, but received %s.", Term.VOID, currSymb.token));
				return typeNode;
			case BOOL:
				add(typeNode, Term.BOOL, String.format("Expected symbol %s, but received %s.", Term.BOOL, currSymb.token));
				return typeNode;
			case CHAR:
				add(typeNode, Term.CHAR, String.format("Expected symbol %s, but received %s.", Term.CHAR, currSymb.token));
				return typeNode;
			case INT:
				add(typeNode, Term.INT, String.format("Expected symbol %s, but received %s.", Term.INT, currSymb.token));
				return typeNode;		
			case ARR:{
				add(typeNode, Term.ARR, String.format("Expected symbol %s, but received %s.", Term.ARR, currSymb.token));
				add(typeNode, Term.LBRACKET, String.format("Expected symbol %s, but received %s.", Term.LBRACKET, currSymb.token));
				typeNode.add(parseExpr());
				add(typeNode, Term.RBRACKET, String.format("Expected symbol %s, but received %s.", Term.RBRACKET, currSymb.token));
				typeNode.add(parseType());
				return typeNode;
			}
			case REC:{
				add(typeNode, Term.REC, String.format("Expected symbol %s, but received %s.", Term.REC, currSymb.token));
				add(typeNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
				typeNode.add(parseArgs());
				add(typeNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
				
				return typeNode;
			}
			case PTR:{
				add(typeNode, Term.PTR, String.format("Expected symbol %s, but received %s.", Term.PTR, currSymb.token));
				typeNode.add(parseType());
				return typeNode;
			}
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	// TODO: Think about using StmtsRest and have Stmts be the top one. Like in args
	private DerNode parseStmt() {
		DerNode stmtNode = new DerNode(Nont.Stmt);
		switch (currSymb.token) {
		case IF:
			add(stmtNode, Term.IF, String.format("Expected symbol %s, but received %s.", Term.IF, currSymb.token));
			stmtNode.add(parseExpr());
			add(stmtNode, Term.THEN, String.format("Expected symbol %s, but received %s.", Term.THEN, currSymb.token));
			stmtNode.add(parseStmt());
			stmtNode.add(parseStmts());
			stmtNode.add(parseElse());
			add(stmtNode, Term.END, String.format("Expected symbol %s, but received %s.", Term.END, currSymb.token));
			add(stmtNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
			return stmtNode;
		case IDENTIFIER:
		case LPARENTHESIS:
		case ADD:
		case SUB:
		case NOT:
		case DATA:
		case ADDR:
		case NEW:
		case DEL: 
		case VOIDCONST:
		case BOOLCONST:
		case INTCONST:
		case PTRCONST:
		case STRCONST:
		case CHARCONST:
			stmtNode.add(parseExpr());
			stmtNode.add(parseBody());
			add(stmtNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
			return stmtNode;
			
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
		
	}
	
	private DerNode parseStmts() {
		DerNode stmtNode = new DerNode(Nont.Stmts);
		switch (currSymb.token) {
		case IDENTIFIER:
		case LPARENTHESIS:
		case IF:
		case WHERE:
		case ADD:
		case SUB:
		case NOT:
		case DATA:
		case ADDR:
		case NEW:
		case DEL: 
		case VOIDCONST:
		case BOOLCONST:
		case INTCONST:
		case PTRCONST:
		case STRCONST:
		case CHARCONST:
			stmtNode.add(parseStmt());
			stmtNode.add(parseStmts());
			return stmtNode;	
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseElse() {
		DerNode elseNode = new DerNode(Nont.ElseEps);
		switch (currSymb.token) {
		case END:
			return elseNode;
		case ELSE:
			add(elseNode, Term.ELSE, String.format("Expected symbol %s, but received %s.", Term.ELSE, currSymb.token));
			elseNode.add(parseStmt());
			elseNode.add(parseStmts());
			return elseNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}

	private DerNode parseArgsFun() {
		DerNode argsFunNode = new DerNode(Nont.ArgsFun);
		switch (currSymb.token) {
		case LPARENTHESIS:
			return argsFunNode;
		default:
			add(parseExpr());
			add(parseArgsFunRest());
			return argsFunNode;
		}
	}
	
	private DerNode parseArgsFunRest() {
		DerNode argsFunNode = new DerNode(Nont.ArgsFunRest);
		switch (currSymb.token) {
		case COMMA:
			add(argsFunNode, Term.COMMA, String.format("Expected symbol %s, but received %s.", Term.COMMA, currSymb.token));
			argsFunNode.add(parseExpr());
			argsFunNode.add(parseArgsFunRest());
			return argsFunNode;		
		case LPARENTHESIS:
				return argsFunNode;
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseWhereEps() {
		DerNode whereEpsNode = new DerNode(Nont.WhereEps);
		switch (currSymb.token) {
		case WHERE:
			add(whereEpsNode, Term.WHERE, String.format("Expected symbol %s, but received %s.", Term.WHERE, currSymb.token));
			whereEpsNode.add(parseDecl());
			whereEpsNode.add(parseDecls());
			return whereEpsNode;		
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	// Entry for expressions
	private DerNode parseExpr(){
		DerNode exprNode = new DerNode(Nont.Expr);
		exprNode.add(parseDisjExpr());
		return exprNode;
	}
	
	private DerNode parseDisjExpr() {
		return null;
	}
	
	
	
	
}
