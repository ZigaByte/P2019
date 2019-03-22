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

/*	*//**
	 * Appends the current symbol in the lookahead buffer to a derivation tree node
	 * (typically the node of the derivation tree that is currently being expanded
	 * by the parser) and replaces the current symbol (just added) with the next
	 * input symbol.
	 * 
	 * @param node The node of the derivation tree currently being expanded by the
	 *             parser.
	 *//*
	private void add(DerNode node) {
		if (currSymb == null)
			throw new Report.InternalError();
		node.add(new DerLeaf(currSymb));
		currSymb = lexAn.lexer();
	}
*/
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

	private DerNode parseDecls(){
		DerNode declsNode = new DerNode(DerNode.Nont.Decls);
		switch (currSymb.token) {
			default:		
				declsNode.add(parseDecl());
				declsNode.add(parseDeclsRest());
				return declsNode;
		}
	}
	
	private DerNode parseDeclsRest(){
		DerNode declsNode = new DerNode(DerNode.Nont.DeclsRest);
		switch (currSymb.token) {
			case RBRACE:
			case EOF:
				return declsNode;
			default:		
				declsNode.add(parseDecl());
				declsNode.add(parseDeclsRest());
				return declsNode;
		}
	}

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
				declNode.add(parseParDeclsEps());
				add(declNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
				add(declNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
				declNode.add(parseType());
				declNode.add(parseBody());
				add(declNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
				
				break;
			}	
			default:
				throw new Report.Error(currSymb, String.format("[parseDecl] Symbol %s (%s) not expected.", currSymb, currSymb.token));
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
			throw new Report.Error(currSymb, String.format("[parseBody] Symbol %s not expected.", currSymb));
		}
	}
	
	private DerNode parseParDecl() {
		DerNode argNode = new DerNode(Nont.ParDecl);
		switch (currSymb.token) {
		case IDENTIFIER:
			add(argNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
			add(argNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
			argNode.add(parseType());
			return argNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseArg] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseParDeclRest() {
		DerNode argsNode = new DerNode(Nont.ParDeclsRest);
		switch (currSymb.token) {
		case COMMA:
			add(argsNode, Term.COMMA, String.format("Expected symbol %s, but received %s.", Term.COMMA, currSymb.token));
			argsNode.add(parseParDecl());
			argsNode.add(parseParDeclRest());
			return argsNode;
		case RPARENTHESIS:
			return argsNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseArgsRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseParDecls() {
		DerNode argsNode = new DerNode(Nont.ParDecls);
		switch (currSymb.token) {
		case IDENTIFIER:
			argsNode.add(parseParDecl());
			argsNode.add(parseParDeclRest());
			return argsNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseArgs] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseParDeclsEps() {
		DerNode argsNode = new DerNode(Nont.ParDeclsEps);
		switch (currSymb.token) {
		case IDENTIFIER:
			argsNode.add(parseParDecl());
			argsNode.add(parseParDeclRest());
			return argsNode;
		case RPARENTHESIS:
			return argsNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseArgs] Symbol %s (%s) not expected.", currSymb, currSymb.token));
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
				typeNode.add(parseParDecls());
				add(typeNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
				
				return typeNode;
			}
			case PTR:{
				add(typeNode, Term.PTR, String.format("Expected symbol %s, but received %s.", Term.PTR, currSymb.token));
				typeNode.add(parseType());
				return typeNode;
			}
		default:
			throw new Report.Error(currSymb, String.format("[parseType] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
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
		case WHILE: 
			add(stmtNode, Term.WHILE, String.format("Expected symbol %s, but received %s.", Term.WHILE, currSymb.token));
			stmtNode.add(parseExpr());
			add(stmtNode, Term.DO, String.format("Expected symbol %s, but received %s.", Term.DO, currSymb.token));
			stmtNode.add(parseStmt());
			stmtNode.add(parseStmts());	
			add(stmtNode, Term.END, String.format("Expected symbol %s, but received %s.", Term.END, currSymb.token));
			add(stmtNode, Term.SEMIC, String.format("Expected symbol %s, but received %s.", Term.SEMIC, currSymb.token));
			return stmtNode;
		case IDENTIFIER:
		case LPARENTHESIS:
		case LBRACE:
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
			throw new Report.Error(currSymb, String.format("[parseStmt] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
		
	}
	
	private DerNode parseStmts() {
		DerNode stmtNode = new DerNode(Nont.Stmts);
		switch (currSymb.token) {
		case IDENTIFIER:
		case LPARENTHESIS:
		case IF:
		case WHILE:
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
			
		case COLON:
		case END:
		case ELSE:
			return stmtNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseStmts] Symbol %s (%s) not expected.", currSymb, currSymb.token));
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
			throw new Report.Error(currSymb, String.format("[parseElse] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}

	private DerNode parseArgsFun() {
		DerNode argsFunNode = new DerNode(Nont.ArgsFun);
		switch (currSymb.token) {
		case RPARENTHESIS:
			return argsFunNode;
		default:
			argsFunNode.add(parseExpr());
			argsFunNode.add(parseArgsFunRest());
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
		case RPARENTHESIS:
				return argsFunNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseArgsFunRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
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
		case RBRACE:
			return whereEpsNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseWhereEps] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	// Entry for expressions
	private DerNode parseExpr(){
		DerNode exprNode = new DerNode(Nont.Expr);
		exprNode.add(parseDisjExpr());
		return exprNode;
	}
	
	
	private DerNode parseDisjExpr() {
		DerNode exprNode = new DerNode(Nont.DisjExpr);
		exprNode.add(parseConjExpr());
		exprNode.add(parseDisjExprRest());
		return exprNode;
	}
	
	private DerNode parseDisjExprRest() {
		DerNode exprNode = new DerNode(Nont.DisjExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
			return exprNode;
		case IOR:
			add(exprNode, Term.IOR, String.format("Expected symbol %s, but received %s.", Term.IOR, currSymb.token));
			exprNode.add(parseConjExpr());
			exprNode.add(parseDisjExprRest());
			return exprNode;
		case XOR:
			add(exprNode, Term.XOR, String.format("Expected symbol %s, but received %s.", Term.XOR, currSymb.token));
			exprNode.add(parseConjExpr());
			exprNode.add(parseDisjExprRest());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseDisjExprRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseConjExpr() {
		DerNode exprNode = new DerNode(Nont.ConjExpr);
		exprNode.add(parseRelExpr());
		exprNode.add(parseConjExprRest());
		return exprNode;
	}
	
	private DerNode parseConjExprRest() {
		DerNode exprNode = new DerNode(Nont.ConjExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
			return exprNode;
		case AND:
			add(exprNode, Term.AND, String.format("Expected symbol %s, but received %s.", Term.AND, currSymb.token));
			exprNode.add(parseRelExpr());
			exprNode.add(parseConjExprRest());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseConjExprRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseRelExpr() {
		DerNode exprNode = new DerNode(Nont.RelExpr);
		exprNode.add(parseAddExpr());
		exprNode.add(parseRelExprRest());
		return exprNode;
	}
	
	private DerNode parseRelExprRest() {
		DerNode exprNode = new DerNode(Nont.RelExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
		case AND:
			return exprNode;
		case EQU:
			add(exprNode, Term.EQU, String.format("Expected symbol %s, but received %s.", Term.EQU, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		case NEQ:
			add(exprNode, Term.NEQ, String.format("Expected symbol %s, but received %s.", Term.NEQ, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		case LTH:
			add(exprNode, Term.LTH, String.format("Expected symbol %s, but received %s.", Term.LTH, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		case GTH:
			add(exprNode, Term.GTH, String.format("Expected symbol %s, but received %s.", Term.GTH, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		case GEQ:
			add(exprNode, Term.GEQ, String.format("Expected symbol %s, but received %s.", Term.GEQ, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		case LEQ:
			add(exprNode, Term.LEQ, String.format("Expected symbol %s, but received %s.", Term.LEQ, currSymb.token));
			exprNode.add(parseAddExpr());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseRelExprRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseAddExpr() {
		DerNode exprNode = new DerNode(Nont.AddExpr);
		exprNode.add(parseMulExpr());
		exprNode.add(parseAddExprRest());
		return exprNode;
	}
	
	private DerNode parseAddExprRest() {
		DerNode exprNode = new DerNode(Nont.AddExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
		case AND:		
		case EQU:
		case NEQ:
		case LTH:
		case GTH:
		case GEQ:
		case LEQ:
			return exprNode;
		case ADD:
			add(exprNode, Term.ADD, String.format("Expected symbol %s, but received %s.", Term.ADD, currSymb.token));
			exprNode.add(parseMulExpr());
			exprNode.add(parseAddExprRest());
			return exprNode;
		case SUB:
			add(exprNode, Term.SUB, String.format("Expected symbol %s, but received %s.", Term.SUB, currSymb.token));
			exprNode.add(parseMulExpr());
			exprNode.add(parseAddExprRest());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseAddExprRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseMulExpr() {
		DerNode exprNode = new DerNode(Nont.MulExpr);
		exprNode.add(parsePrefExpr());
		exprNode.add(parseMulExprRest());
		return exprNode;
	}
	
	private DerNode parseMulExprRest() {
		DerNode exprNode = new DerNode(Nont.MulExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
		case AND:		
		case EQU:
		case NEQ:
		case LTH:
		case GTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
			return exprNode;
		case MUL:
			add(exprNode, Term.MUL, String.format("Expected symbol %s, but received %s.", Term.MUL, currSymb.token));
			exprNode.add(parsePrefExpr());
			exprNode.add(parseMulExprRest());
			return exprNode;
		case DIV:
			add(exprNode, Term.DIV, String.format("Expected symbol %s, but received %s.", Term.DIV, currSymb.token));
			exprNode.add(parsePrefExpr());
			exprNode.add(parseMulExprRest());
			return exprNode;
		case MOD:
			add(exprNode, Term.MOD, String.format("Expected symbol %s, but received %s.", Term.MOD, currSymb.token));
			exprNode.add(parsePrefExpr());
			exprNode.add(parseMulExprRest());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseMulExprRest] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parsePrefExpr() {
		DerNode exprNode = new DerNode(Nont.PrefExpr);
		switch (currSymb.token) {
		case LPARENTHESIS:
		case IDENTIFIER:
		case LBRACE:
		case VOIDCONST:
		case BOOLCONST:
		case INTCONST:
		case PTRCONST:
		case STRCONST:
		case CHARCONST:	
			exprNode.add(parsePstfExpr());
			return exprNode;
		case ADD:
			add(exprNode, Term.ADD, String.format("Expected symbol %s, but received %s.", Term.ADD, currSymb.token));
			exprNode.add(parsePrefExpr());
			return exprNode;
		case SUB:
			add(exprNode, Term.SUB, String.format("Expected symbol %s, but received %s.", Term.SUB, currSymb.token));
			exprNode.add(parsePrefExpr());
			return exprNode;
		case NOT:
			add(exprNode, Term.NOT, String.format("Expected symbol %s, but received %s.", Term.NOT, currSymb.token));
			exprNode.add(parsePrefExpr());
			return exprNode;
		case DATA:
			add(exprNode, Term.DATA, String.format("Expected symbol %s, but received %s.", Term.DATA, currSymb.token));
			exprNode.add(parsePrefExpr());
			return exprNode;
		case ADDR:
			add(exprNode, Term.ADDR, String.format("Expected symbol %s, but received %s.", Term.ADDR, currSymb.token));
			exprNode.add(parsePrefExpr());
			return exprNode;

		case NEW:
			add(exprNode, Term.NEW, String.format("Expected symbol %s, but received %s.", Term.NEW, currSymb.token));
			add(exprNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
			exprNode.add(parseType());
			add(exprNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			return exprNode;

		case DEL:
			add(exprNode, Term.DEL, String.format("Expected symbol %s, but received %s.", Term.DEL, currSymb.token));
			add(exprNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
			exprNode.add(parseExpr());
			add(exprNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parsePrefExpr] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parsePstfExpr() {
		DerNode exprNode = new DerNode(Nont.PstfExpr);
		switch (currSymb.token) {
		case LPARENTHESIS:
		case IDENTIFIER:
		case LBRACE:
		case VOIDCONST:
		case BOOLCONST:
		case INTCONST:
		case PTRCONST:
		case STRCONST:
		case CHARCONST:	
			exprNode.add(parseCastExpr());
			exprNode.add(parsePstfExprRest());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parsePstfExpr] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parsePstfExprRest() {
		DerNode exprNode = new DerNode(Nont.PstfExprRest);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
		case AND:		
		case EQU:
		case NEQ:
		case LTH:
		case GTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
			return exprNode;
		case DOT:
			add(exprNode, Term.DOT, String.format("Expected symbol %s, but received %s.", Term.DOT, currSymb.token));
			add(exprNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
			return exprNode;
		case LBRACKET:
			add(exprNode, Term.LBRACKET, String.format("Expected symbol %s, but received %s.", Term.LBRACKET, currSymb.token));
			exprNode.add(parseExpr());
			add(exprNode, Term.RBRACKET, String.format("Expected symbol %s, but received %s.", Term.RBRACKET, currSymb.token));
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parsePstfExprRest] ExprSymbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseCastExpr() {
		DerNode exprNode = new DerNode(Nont.CastExpr);
		switch (currSymb.token) {
		case IDENTIFIER:
		case LBRACE:
		case VOIDCONST:
		case BOOLCONST:
		case INTCONST:
		case PTRCONST:
		case STRCONST:
		case CHARCONST:	
			exprNode.add(parseAtomExpr());
			return exprNode;
		case LPARENTHESIS:
			add(exprNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
			exprNode.add(parseExpr());
			exprNode.add(parseCastEps());
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseCastExpr] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseAtomExpr() {
		DerNode exprNode = new DerNode(Nont.AtomExpr);
		switch (currSymb.token) {
		case IDENTIFIER:
			add(exprNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
			exprNode.add(parseCallEps());
			return exprNode;
		case VOIDCONST:			
			add(exprNode, Term.VOIDCONST, String.format("Expected symbol %s, but received %s.", Term.VOIDCONST, currSymb.token));
			return exprNode;
		case BOOLCONST:			
			add(exprNode, Term.BOOLCONST, String.format("Expected symbol %s, but received %s.", Term.BOOLCONST, currSymb.token));
			return exprNode;
		case INTCONST:			
			add(exprNode, Term.INTCONST, String.format("Expected symbol %s, but received %s.", Term.INTCONST, currSymb.token));
			return exprNode;
		case PTRCONST:			
			add(exprNode, Term.PTRCONST, String.format("Expected symbol %s, but received %s.", Term.PTRCONST, currSymb.token));
			return exprNode;
		case STRCONST:			
			add(exprNode, Term.STRCONST, String.format("Expected symbol %s, but received %s.", Term.STRCONST, currSymb.token));
			return exprNode;
		case CHARCONST:	
			add(exprNode, Term.CHARCONST, String.format("Expected symbol %s, but received %s.", Term.CHARCONST, currSymb.token));
			return exprNode;
			
		case LBRACE:
			add(exprNode, Term.LBRACE, String.format("Expected symbol %s, but received %s.", Term.LBRACE, currSymb.token));
			exprNode.add(parseStmt());
			exprNode.add(parseStmts());
			add(exprNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
			exprNode.add(parseExpr());
			exprNode.add(parseWhereEps());
			add(exprNode, Term.RBRACE, String.format("Expected symbol %s, but received %s.", Term.RBRACE, currSymb.token));
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseAtomExpr] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseCastEps() {
		DerNode exprNode = new DerNode(Nont.CastEps);
		switch (currSymb.token) {
		case COLON: 
			add(exprNode, Term.COLON, String.format("Expected symbol %s, but received %s.", Term.COLON, currSymb.token));
			exprNode.add(parseType());
			add(exprNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			return exprNode;
		case RPARENTHESIS:
			add(exprNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseCastEps] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
	
	private DerNode parseCallEps() {
		DerNode exprNode = new DerNode(Nont.CallEps);
		switch (currSymb.token) {
		case COLON:
		case SEMIC:	
		case RPARENTHESIS:
		case ASSIGN:
		case COMMA:
		case RBRACKET:
		case THEN:
		case DO:
		case WHERE:
		case RBRACE:
		case IOR:
		case XOR:
		case AND:		
		case EQU:
		case NEQ:
		case LTH:
		case GTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		case DOT:
		case LBRACKET:
			return exprNode;
		case LPARENTHESIS:
			add(exprNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
			exprNode.add(parseArgsFun());
			add(exprNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			return exprNode;
		default:
			throw new Report.Error(currSymb, String.format("[parseCallEps] Symbol %s (%s) not expected.", currSymb, currSymb.token));
		}
	}
}
