/**
 * @author sliva
 */
package compiler.phases.synan;

import java.util.ArrayList;

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
		declsNode.add(parseDecl());
		declsNode.add(parseDeclsRest());
		return declsNode;
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
				break;
			}
			case FUN:{
				break;
			}	
			default:
				throw new Report.Error(currSymb, String.format("Symbol %s not expected.", currSymb));
		}
		return declNode;
	}
	
	// TODO: Other declarations
	private DerNode parseDeclsRest(){
		return new DerNode(Nont.ParDeclsEps);
	}

	// type -> void | bool | char | int
	// type -> arr [expr] type
	// type -> rec (identifier:type {,identifier:type})
	// type -> ptr type
	// type -> identifier
	// type -> (type)
	private DerNode parseType(){
		DerNode typeNode = new DerNode(Nont.Type);
		switch(currSymb.token){
		case VOID:
			add(typeNode, Term.VOID, String.format("Expected symbol %s, but received %s.", Term.VOID, currSymb.token));
			break;
		case BOOL:
			add(typeNode, Term.BOOL, String.format("Expected symbol %s, but received %s.", Term.BOOL, currSymb.token));
			break;
		case CHAR:
			add(typeNode, Term.CHAR, String.format("Expected symbol %s, but received %s.", Term.CHAR, currSymb.token));
			break;
		case INT:
			add(typeNode, Term.INT, String.format("Expected symbol %s, but received %s.", Term.INT, currSymb.token));
			break;		
		case ARR:{
			add(typeNode, Term.ARR, String.format("Expected symbol %s, but received %s.", Term.ARR, currSymb.token));
			add(typeNode, Term.LBRACKET, String.format("Expected symbol %s, but received %s.", Term.LBRACKET, currSymb.token));
			typeNode.add(parseExpr());
			add(typeNode, Term.RBRACKET, String.format("Expected symbol %s, but received %s.", Term.RBRACKET, currSymb.token));
			typeNode.add(parseType());
			break;
		}
		case REC:{
			// TODO: What is this????
			break;
		}	
		case PTR:{
			add(typeNode, Term.PTR, String.format("Expected symbol %s, but received %s.", Term.PTR, currSymb.token));
			typeNode.add(parseType());
			break;
		}
		case IDENTIFIER:
			add(typeNode, Term.IDENTIFIER, String.format("Expected symbol %s, but received %s.", Term.IDENTIFIER, currSymb.token));
			break;
		case LPARENTHESIS:{
			add(typeNode, Term.LPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.LPARENTHESIS, currSymb.token));
			typeNode.add(parseType());
			add(typeNode, Term.RPARENTHESIS, String.format("Expected symbol %s, but received %s.", Term.RPARENTHESIS, currSymb.token));
			break;
		}
		default:
			throw new Report.Error(currSymb, String.format("Symbol %s not expected.", currSymb));
	}
		
		
		
		return typeNode;
	}
	
	private DerNode parseExpr(){
		DerNode exprNode = new DerNode(Nont.Expr);
		return exprNode;
	}
	
	
	
	
}
