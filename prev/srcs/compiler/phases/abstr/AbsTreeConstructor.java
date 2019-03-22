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
				return new AbsFunDecl(new Location(node), name, parDecls, type);
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
		
		case Expr:{
			return new AbsAtomExpr(new Location(node,node ), AbsAtomExpr.Type.INT, "TODO");
		}
		
		
		default: System.out.println(node.label);
		}
		return null;
	}
}
