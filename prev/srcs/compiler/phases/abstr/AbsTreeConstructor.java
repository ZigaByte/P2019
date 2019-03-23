/**
 * @author sliva
 */
package compiler.phases.abstr;

import java.util.*;

import compiler.common.report.*;
import compiler.data.dertree.*;
import compiler.data.dertree.visitor.*;
import compiler.data.abstree.*;
import compiler.data.abstree.AbsAtomType.Type;
import compiler.data.abstree.AbsBinExpr.Oper;

/**
 * Transforms a derivation tree to an abstract syntax tree.
 * 
 * @author sliva
 */
public class AbsTreeConstructor implements DerVisitor<AbsTree, AbsTree> {

	@Override
	public AbsTree visit(DerLeaf leaf, AbsTree visArg) {
		throw new Report.InternalError();
	}

	@Override
	public AbsTree visit(DerNode node, AbsTree visArg) {
		switch (node.label) {

		case Source: {
			AbsDecls decls = (AbsDecls) node.subtree(0).accept(this, null);
			return new AbsSource(decls, decls);
		}

		case Decls: {
			Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
			AbsDecl decl = (AbsDecl) node.subtree(0).accept(this, null);
			allDecls.add(decl);
			AbsDecls decls = (AbsDecls) node.subtree(1).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.decls());
			return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}

		case DeclsRest: {
			if (node.numSubtrees() == 0)
				return null;
			Vector<AbsDecl> allDecls = new Vector<AbsDecl>();
			AbsDecl decl = (AbsDecl) node.subtree(0).accept(this, null);
			allDecls.add(decl);
			AbsDecls decls = (AbsDecls) node.subtree(1).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.decls());
			return new AbsDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		
		case Decl: {
			DerLeaf nodeType = ((DerLeaf)node.subtree(0));
			switch (nodeType.symb.token) {
			case TYP:{
				AbsType type = (AbsType) node.subtree(3).accept(this, null);
				String name = ((DerLeaf)(node.subtree(1))).symb.lexeme;
				return new AbsTypDecl(new Location(type, type), name, type);
			}
			case VAR:{
				AbsType type = (AbsType) node.subtree(3).accept(this, null);
				String name = ((DerLeaf)(node.subtree(1))).symb.lexeme;
				return new AbsVarDecl(new Location(type, type), name, type);
			}
			case FUN:
				String name = ((DerLeaf)(node.subtree(1))).symb.lexeme;
				AbsParDecls parDecls = (AbsParDecls) node.subtree(3).accept(this, null);
				AbsType type = (AbsType) node.subtree(6).accept(this, null);
				if(((DerNode)node.subtree(7)).numSubtrees() == 0) {
					return new AbsFunDecl(new Location(node), name, parDecls, type);
				}
				AbsExpr expr = (AbsExpr) node.subtree(7).accept(this, null);
				return new AbsFunDef(new Location(node), name, parDecls, type, expr);
				
				default:
				System.out.println("Something went wrong");
			}
		}
		
		case Type:{
			// Switch between different types of type
			switch (((DerLeaf)(node.subtree(0))).symb.token) {
			case VOID:
				return new AbsAtomType(new Location(node, node), Type.VOID);
			case BOOL:
				return new AbsAtomType(new Location(node, node), Type.BOOL);
			case CHAR:
				return new AbsAtomType(new Location(node, node), Type.CHAR);
			case INT:
				return new AbsAtomType(new Location(node, node), Type.INT);
			case ARR: {
				AbsExpr expr = (AbsExpr) node.subtree(2).accept(this, null);
				AbsType type = (AbsType) node.subtree(4).accept(this, null);
				return new AbsArrType(new Location(node, node), expr, type);
			}
			case REC:{
				AbsCompDecls params = (AbsCompDecls) node.subtree(2).accept(this, null);
				return new AbsRecType(new Location(node, node), params);
			}
			
			case PTR: {
				AbsType type = (AbsType) node.subtree(1).accept(this, null);
				return new AbsPtrType(new Location(node,node), type);
			}
			case IDENTIFIER:{
				String name = ((DerLeaf)(node.subtree(0))).symb.lexeme;
				return new AbsTypName(new Location(node, node), name);
			}
			case LPARENTHESIS:{
				return node.subtree(1).accept(this, null);
			}
			
			default:
				return new AbsAtomType(new Location(node, node), Type.INT);
			}
		}		
		case CompDecls:{
			Vector<AbsCompDecl> allDecls = new Vector<AbsCompDecl>();
			AbsCompDecl decl = (AbsCompDecl) node.subtree(0).accept(this, null);
			allDecls.add(decl);
			AbsCompDecls decls = (AbsCompDecls) node.subtree(1).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.compDecls());
			return new AbsCompDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		case CompDecl:{
			String name = ((DerLeaf)(node.subtree(0))).symb.lexeme;
			AbsType type = (AbsType) node.subtree(2).accept(this, null);
			return new AbsCompDecl(new Location(node),  name, type);
		}
		case CompDeclsRest:{
			if (node.numSubtrees() == 0)
				return null;
			Vector<AbsCompDecl> allDecls = new Vector<AbsCompDecl>();
			AbsCompDecl decl = (AbsCompDecl) node.subtree(1).accept(this, null);
			allDecls.add(decl);
			AbsCompDecls decls = (AbsCompDecls) node.subtree(2).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.compDecls());
			return new AbsCompDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		
		case ParDecls:{
			Vector<AbsParDecl> allDecls = new Vector<AbsParDecl>();
			AbsParDecl decl = (AbsParDecl) node.subtree(0).accept(this, null);
			allDecls.add(decl);
			AbsParDecls decls = (AbsParDecls) node.subtree(1).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.parDecls());
			return new AbsParDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		case ParDecl:{
			String name = ((DerLeaf)(node.subtree(0))).symb.lexeme;
			AbsType type = (AbsType) node.subtree(2).accept(this, null);
			return new AbsParDecl(new Location(node),  name, type);
		}
		case ParDeclsRest:{
			if (node.numSubtrees() == 0)
				return null;
			Vector<AbsParDecl> allDecls = new Vector<AbsParDecl>();
			AbsParDecl decl = (AbsParDecl) node.subtree(1).accept(this, null);
			allDecls.add(decl);
			AbsParDecls decls = (AbsParDecls) node.subtree(2).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.parDecls());
			return new AbsParDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		
		case ParDeclsEps:{
			if (node.numSubtrees() == 0)
				return new AbsParDecls(new Location(1,1,1,1), new Vector<AbsParDecl>());;
			Vector<AbsParDecl> allDecls = new Vector<AbsParDecl>();
			AbsParDecl decl = (AbsParDecl) node.subtree(0).accept(this, null);
			allDecls.add(decl);
			AbsParDecls decls = (AbsParDecls) node.subtree(1).accept(this, null);
			if (decls != null)
				allDecls.addAll(decls.parDecls());
			return new AbsParDecls(new Location(decl, decls == null ? decl : decls), allDecls);
		}
		
		case BodyEps:{
			return node.subtree(1).accept(this, null);
		}
		
		case Expr:{
			return node.subtree(0).accept(this, null);
		}
		case DisjExpr:{
			AbsExpr fstExpr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, fstExpr);
		}
		
		case DisjExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			Oper operation = Oper.ADD;
			switch(((DerLeaf)node.subtree(0)).symb.token) {
				case IOR:
					operation = Oper.IOR;
					break;
				case XOR:
					operation = Oper.XOR;
					break;
				default: System.out.println("Something went wrong");
			}
			AbsExpr sndExpr = (AbsExpr) node.subtree(1).accept(this, null);
			
			AbsBinExpr binExpr = new AbsBinExpr(new Location(node), operation, (AbsExpr) visArg, sndExpr);
			return node.subtree(2).accept(this, binExpr);
		}
		
		case ConjExpr:{
			AbsExpr fstExpr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, fstExpr);
		}
		
		case ConjExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			AbsExpr sndExpr = (AbsExpr) node.subtree(1).accept(this, null);
			
			AbsBinExpr binExpr = new AbsBinExpr(new Location(node), Oper.AND, (AbsExpr) visArg, sndExpr);
			return node.subtree(2).accept(this, binExpr);
		}
		
		case RelExpr:{
			AbsExpr fstExpr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, fstExpr);
		}
		
		case RelExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			Oper operation = Oper.ADD;
			switch(((DerLeaf)node.subtree(0)).symb.token) {
				case EQU:
					operation = Oper.EQU;
					break;
				case GTH:
					operation = Oper.GTH;
					break;
				case NEQ:
					operation = Oper.NEQ;
					break;
				case LTH:
					operation = Oper.LTH;
					break;
				case LEQ:
					operation = Oper.LEQ;
					break;
				case GEQ:
					operation = Oper.GEQ;
					break;
				default: System.out.println("Something went wrong");
			}
			AbsExpr sndExpr = (AbsExpr) node.subtree(1).accept(this, null);
			return new AbsBinExpr(new Location(node), operation, (AbsExpr) visArg, sndExpr);
		}
		case AddExpr:{
			AbsExpr fstExpr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, fstExpr);
		}
		
		case AddExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			Oper operation = Oper.ADD;
			switch(((DerLeaf)node.subtree(0)).symb.token) {
				case ADD:
					operation = Oper.ADD;
					break;
				case SUB:
					operation = Oper.SUB;
					break;
				default: System.out.println("Something went wrong");
			}
			AbsExpr sndExpr = (AbsExpr) node.subtree(1).accept(this, null);
			
			AbsBinExpr binExpr = new AbsBinExpr(new Location(node), operation, (AbsExpr) visArg, sndExpr);
			return node.subtree(2).accept(this, binExpr);
		}
		
		case MulExpr:{
			AbsExpr fstExpr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, fstExpr);
		}
		
		case MulExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			Oper operation = Oper.MUL;
			switch(((DerLeaf)node.subtree(0)).symb.token) {
				case MUL:
					operation = Oper.MUL;
					break;
				case DIV:
					operation = Oper.DIV;
					break;
				case MOD:
					operation = Oper.MOD;
					break;
				default: System.out.println("Something went wrong");
			}
			AbsExpr sndExpr = (AbsExpr) node.subtree(1).accept(this, null);
			
			AbsBinExpr binExpr = new AbsBinExpr(new Location(node), operation, (AbsExpr) visArg, sndExpr);
			return node.subtree(2).accept(this, binExpr);
		}
		
		case PrefExpr:{
			if(node.numSubtrees() == 1) {
				return node.subtree(0).accept(this, null);
			}
			if(node.numSubtrees() == 2) {
				AbsUnExpr.Oper operation = AbsUnExpr.Oper.NOT;
				switch(((DerLeaf)node.subtree(0)).symb.token) {
					case NOT:
						operation = AbsUnExpr.Oper.NOT;
						break;
					case ADD:
						operation = AbsUnExpr.Oper.ADD;
						break;
					case SUB:
						operation = AbsUnExpr.Oper.SUB;
						break;
					case ADDR:
						operation = AbsUnExpr.Oper.ADDR;
						break;
					case DATA:
						operation = AbsUnExpr.Oper.DATA;
						break;
					default: System.out.println("Something went wrong");
				}
				AbsExpr expr = (AbsExpr)node.subtree(1).accept(this, null);
				return new AbsUnExpr(new Location(node), operation, expr);
			} else {
				switch(((DerLeaf)node.subtree(0)).symb.token) {
				case NEW:
					AbsType type = (AbsType) node.subtree(2).accept(this, null); 
					return new AbsNewExpr(new Location(node), type);
				case DEL:
					AbsExpr expr = (AbsExpr) node.subtree(2).accept(this, null); 
					return new AbsDelExpr(new Location(node), expr);
				default: System.out.println("Something wrong?");
				}
			}
		}
		
		case PstfExpr:{
			AbsExpr expr = (AbsExpr) node.subtree(0).accept(this, null);
			return node.subtree(1).accept(this, expr);
		}
		
		case PstfExprRest:{
			if(node.numSubtrees() == 0) {
				return visArg;
			}
			switch (((DerLeaf)node.subtree(0)).symb.token) {
			case LBRACKET:
				AbsExpr index = (AbsExpr) node.subtree(1).accept(this, null);
				return new AbsArrExpr(new Location(node), (AbsExpr) visArg, index);

			case DOT:
				String name = ((DerLeaf)node.subtree(1)).symb.lexeme;
				AbsVarName comp = new AbsVarName(new Location(node), name);
				return new AbsRecExpr(new Location(node), (AbsExpr) visArg, comp);
			default:
				break;
			}
		}
		
		case CastExpr:{
			if(node.numSubtrees() == 1) {
				return node.subtree(0).accept(this, null);
			}
			AbsExpr expr = (AbsExpr) node.subtree(1).accept(this, null);
			return node.subtree(2).accept(this, expr);
		}
		
		case CastEps:{
			if(node.numSubtrees() <= 1) {
				return visArg;
			}
			AbsType type = (AbsType) node.subtree(1).accept(this, null);
			return new AbsCastExpr(new Location(node), (AbsExpr) visArg, type);
		}
		
		case AtomExpr:{
			switch(((DerLeaf)node.subtree(0)).symb.token) {
			case IDENTIFIER:
	
				break;
			case INTCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.INT, ((DerLeaf)node.subtree(0)).symb.lexeme);
			case CHARCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.CHAR, ((DerLeaf)node.subtree(0)).symb.lexeme);
			case BOOLCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.BOOL, ((DerLeaf)node.subtree(0)).symb.lexeme);
			case STRCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.STR, ((DerLeaf)node.subtree(0)).symb.lexeme);
			case VOIDCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.VOID, ((DerLeaf)node.subtree(0)).symb.lexeme);
			case PTRCONST:
				return new AbsAtomExpr(new Location(node), AbsAtomExpr.Type.PTR, ((DerLeaf)node.subtree(0)).symb.lexeme);
			default: System.out.println("Something went wrong");;
			}
			
			
		}
		
		default: System.out.println(node.label);
		}
		return null;
	}
}
