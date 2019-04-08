/**
 * @author sliva 
 */
package compiler.phases.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.phases.seman.SymbTable.CannotFndNameException;
import compiler.phases.seman.SymbTable.CannotInsNameException;

/**
 * Type resolving: the result is stored in {@link SemAn#declaresType},
 * {@link SemAn#isType}, and {@link SemAn#ofType}.
 * 
 * @author sliva
 */
public class TypeResolver extends AbsFullVisitor<SemType, TypeResolver.Phase> {

	enum Phase {
		DECLARES_TYPE_CREATION, DECLARES_TYPE_LINKING, DECLARES_CHECK, EXPR_LINK
	}

	/** Symbol tables of individual record types. */
	private final HashMap<SemRecType, SymbTable> symbTables = new HashMap<SemRecType, SymbTable>();

	@Override
	public SemType visit(AbsSource source, Phase visArg) {
		super.visit(source, Phase.DECLARES_TYPE_CREATION);
		super.visit(source, Phase.DECLARES_TYPE_LINKING);
		super.visit(source, Phase.DECLARES_CHECK);
		super.visit(source, Phase.EXPR_LINK);
		return null;
	}

	@Override
	public SemType visit(AbsTypDecl typDecl, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_CREATION) {
			SemAn.declaresType.put(typDecl, new SemNamedType(typDecl.name));
		}else if (visArg == Phase.DECLARES_TYPE_LINKING) {
			SemNamedType namedType = SemAn.declaresType.get(typDecl);
			namedType.define(typDecl.type.accept(this, Phase.DECLARES_TYPE_LINKING));
		}

		return super.visit(typDecl, visArg);
	}
	
	@Override
	public SemType visit(AbsParDecl parDecl, Phase visArg) {
		return parDecl.type.accept(this, visArg);
	}
	
	@Override
	public SemType visit(AbsVarDecl varDecl, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType newType = varDecl.type.accept(this, visArg);
			if(newType instanceof SemVoidType) {
				throw new Report.Error(varDecl.location(), "[typeResolving] Variable may not be of type void");
			}
			return newType;
		}
		return super.visit(varDecl, visArg);
	}
	
	@Override
	public SemType visit(AbsVarName varName, Phase visArg) {
		return SemAn.declaredAt.get(varName).accept(this, visArg);
	}
	
	@Override
	public SemType visit(AbsAtomType atomType, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			SemType newType = null;
			switch (atomType.type) {
			case INT:
				newType = new SemIntType();
				break;
			case BOOL:
				newType = new SemBoolType();
				break;
			case CHAR:
				newType = new SemCharType();
				break;
			case VOID:
				newType = new SemVoidType();
				break;
			}
			SemAn.isType.put(atomType, newType);
			return newType;
		} else if(visArg == Phase.EXPR_LINK){
			return SemAn.isType.get(atomType);
		}
		return super.visit(atomType, visArg);
	}

	@Override
	public SemType visit(AbsArrType arrType, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			if (arrType.len instanceof AbsAtomExpr) {
				try {
					int len = Integer.parseInt(((AbsAtomExpr) arrType.len).expr);
					SemType elementType = arrType.elemType.accept(this, visArg);
					
					if (elementType instanceof SemVoidType) {
						throw new Report.Error(arrType, String.format("[typeResolving] Void type not allowed in array."));
					}
					SemType newType = new SemArrType(len, elementType);
					SemAn.isType.put(arrType, newType);
					return newType;
				}catch (Exception e) {
					throw new Report.Error(arrType, String.format("[typeResolving] Array length must be a constant."));
				}
			}
		} else if(visArg == Phase.DECLARES_CHECK) {
			if(((SemArrType)SemAn.isType.get(arrType)).elemType.matches(new SemVoidType())) {
				throw new Report.Error(arrType, String.format("[typeResolving] Void type not allowed in array."));
			}
		} else if(visArg == Phase.EXPR_LINK){
			return SemAn.isType.get(arrType);
		}
		return super.visit(arrType, visArg);
	}

	@Override
	public SemType visit(AbsPtrType ptrType, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			SemType newType = new SemPtrType(ptrType.ptdType.accept(this, visArg));
			SemAn.isType.put(ptrType, newType);
			return newType;
		}else if(visArg == Phase.EXPR_LINK){
			return SemAn.isType.get(ptrType);
		}
		return super.visit(ptrType, visArg);
	}

	@Override
	public SemType visit(AbsRecType recType, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			Vector<SemType> compTypes = new Vector<>();
			for (AbsCompDecl decl : recType.compDecls.compDecls()) {
				SemType elementType = decl.accept(this, Phase.DECLARES_TYPE_LINKING);
				if (elementType instanceof SemVoidType) {
					throw new Report.Error(recType, String.format("[typeResolving] Void type not allowed in record."));
				}
				compTypes.add(elementType);
			}
			SemRecType newType = new SemRecType(compTypes);
			SemAn.isType.put(recType, newType);
			
			// add to symbol array
			SymbTable recTable = new SymbTable();
			for (AbsCompDecl decl : recType.compDecls.compDecls()) {
				try {
					recTable.ins(decl.name, decl);
				} catch (CannotInsNameException e) {
					throw new Report.Error("[nameResolution] " + recType.location() + " Could not insert " + decl.name + " into a rector declaration.");
				}
			}
			symbTables.put(newType, recTable);
			
			return newType;
		} else if(visArg == Phase.DECLARES_CHECK) {
			SemRecType rec = (SemRecType)SemAn.isType.get(recType);
			for(SemType elementType : rec.compTypes()) {
				if(elementType.matches(new SemVoidType())) {
					throw new Report.Error(recType, String.format("[typeResolving] Void type not allowed in record."));
				}	
			}
		}else if(visArg == Phase.EXPR_LINK){
			return SemAn.isType.get(recType);
		}
		return super.visit(recType, visArg);
	}

	@Override
	public SemType visit(AbsCompDecl compDecl, Phase visArg) {
		return compDecl.type.accept(this, visArg);
	}

	@Override
	public SemType visit(AbsTypName typName, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			SemType newType = SemAn.declaresType.get((AbsTypDecl) SemAn.declaredAt.get(typName));
			SemAn.isType.put(typName, newType);
			return newType;
		}else if(visArg == Phase.EXPR_LINK){
			return SemAn.isType.get(typName);
		}
		return super.visit(typName, visArg);
	}

	@Override
	public SemType visit(AbsAtomExpr atomExpr, Phase visArg) {
		if (visArg == Phase.EXPR_LINK) {
			SemType newType = null;
			switch (atomExpr.type) {
			case VOID:
				newType = new SemVoidType();
				break;
			case PTR:
				newType = new SemPtrType(new SemVoidType());
				break;
			case BOOL:
				newType = new SemBoolType();
				break;
			case CHAR:
				newType = new SemCharType();
				break;
			case INT:
				newType = new SemIntType();
				break;
			case STR:
				// WTFFFF?????? TODO
				break;
			}
			SemAn.ofType.put(atomExpr, newType);
			return newType;
		}
		return super.visit(atomExpr, visArg);
	}

	@Override
	public SemType visit(AbsUnExpr unExpr, Phase visArg) {
		if (visArg == Phase.EXPR_LINK) {
			SemType it = unExpr.subExpr.accept(this, visArg);
			while (it instanceof SemNamedType) {
				it = ((SemNamedType)it).type;
			}
			
			switch (unExpr.oper) {
			case NOT:{
				if(!(it instanceof SemBoolType)) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression after unary ! must be of type bool."));
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			case SUB:
			case ADD:{
				if(!(it instanceof SemIntType)) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression after unary +,- must be of type int."));
				}
				SemType thisType = new SemIntType();
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			case ADDR:{// $
				if(it instanceof SemVoidType) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression after unary $ must not be of type void."));	
				}
				SemType thisType = new SemPtrType(it);
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			case DATA:{// @
				if(!(it instanceof SemPtrType)) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression after unary @ must be a pointer."));	
				}
				SemPtrType ptrType = (SemPtrType) it;
				if(ptrType.ptdType instanceof SemVoidType) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression after unary @ must not be void pointer."));	
				}
				
				SemType thisType = ptrType.ptdType;
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			}
			
			return null;
		}
		return super.visit(unExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsBinExpr binExpr, Phase visArg) {
		if (visArg == Phase.EXPR_LINK) {
			SemType it1 = binExpr.fstExpr.accept(this, visArg);
			SemType it2 = binExpr.sndExpr.accept(this, visArg);
			while (it1 instanceof SemNamedType) {
				it1 = ((SemNamedType)it1).type;
			}
			while (it2 instanceof SemNamedType) {
				it2 = ((SemNamedType)it2).type;
			}
			
			switch (binExpr.oper) {
			case AND:
			case IOR:
			case XOR:{
				if(!(it1 instanceof SemBoolType) || !(it2 instanceof SemBoolType)) {
					throw new Report.Error(binExpr.location(), "[typeResolving] Expressions around &, | or ^ must be of type bool.");
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(binExpr, thisType);
				return thisType; 
			}
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:{
				if(!( (it1 instanceof SemIntType && it2 instanceof SemIntType)
						|| (it1 instanceof SemCharType && it2 instanceof SemCharType) )) {
					throw new Report.Error(binExpr.location(), "[typeResolving] Expressions around +,-,*,/,% must be of type int or char. The two types must be the same.");
				}
				SemType thisType = new SemIntType();
				SemAn.ofType.put(binExpr, thisType);
				return thisType; 
			}
			case EQU:
			case NEQ:{
				if(!( (it1 instanceof SemBoolType && it2 instanceof SemBoolType)
						|| (it1 instanceof SemCharType && it2 instanceof SemCharType)
						|| (it1 instanceof SemIntType && it2 instanceof SemIntType)
						|| (it1 instanceof SemPtrType && it2 instanceof SemPtrType && ((SemPtrType)it1).matches((SemPtrType) it2)))) {
					throw new Report.Error(binExpr.location(), "[typeResolving] Expressions around ==,!= must be of type int, char, bool or ptr. The two types must be the same.");
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(binExpr, thisType);
				return thisType; 
			}
			case GEQ:
			case GTH:
			case LEQ:
			case LTH:{
				if(!( (it1 instanceof SemCharType && it2 instanceof SemCharType)
						|| (it1 instanceof SemIntType && it2 instanceof SemIntType)
						|| (it1 instanceof SemPtrType && it2 instanceof SemPtrType && ((SemPtrType)it1).matches((SemPtrType) it2)))) {
					throw new Report.Error(binExpr.location(), "[typeResolving] Expressions around <,<=,>,>= must be of type int, char, bool or ptr. The two types must be the same.");
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(binExpr, thisType);
				return thisType; 
			}
			}
			
			return null;
		}
		return super.visit(binExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsNewExpr newExpr, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType it = newExpr.type.accept(this, visArg);
			while (it instanceof SemNamedType) {
				it = ((SemNamedType)it).type;
			}
			
			if(it instanceof SemVoidType) {
				throw new Report.Error(newExpr.location(), String.format("[typeResolving] Expression in statement new must not be of type void."));	
			}
			SemType thisType = new SemPtrType(it);
			SemAn.ofType.put(newExpr, thisType);
			
			return thisType; 
		}
		return super.visit(newExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsDelExpr delExpr, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType it = delExpr.expr.accept(this, visArg);
			while (it instanceof SemNamedType) {
				it = ((SemNamedType)it).type;
			}
			
			if(!(it instanceof SemPtrType)) {
				throw new Report.Error(delExpr.location(), String.format("[typeResolving] Expression in statement new must be a pointer."));	
			}
			SemPtrType ptrType = (SemPtrType) it;
			if(ptrType.ptdType instanceof SemVoidType) {
				throw new Report.Error(delExpr.location(), String.format("[typeResolving] Expression in statement new must not be of type void."));	
			}
			
			SemType thisType = new SemVoidType();
			SemAn.ofType.put(delExpr, thisType);
			return thisType; 
		}
		return super.visit(delExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsArrExpr arrExpr, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType arrayType = arrExpr.array.accept(this, visArg);
			while (arrayType instanceof SemNamedType) {
				arrayType = ((SemNamedType)arrayType).type;
			}
		
			SemType index = arrExpr.index.accept(this, visArg);
			while (index instanceof SemNamedType) {
				index = ((SemNamedType)index).type;
			}
			
			if(!(arrayType instanceof SemArrType)) {
				throw new Report.Error(arrExpr.location(), String.format("[typeResolving] Expression before [] must be of type array."));	
			}
			
			if(!(index instanceof SemIntType)) {
				throw new Report.Error(arrExpr.location(), String.format("[typeResolving] Expression for array index must be of type int."));	
			}
			
			SemType thisType = ((SemArrType)arrayType).elemType;
			SemAn.ofType.put(arrExpr, thisType);
			return thisType; 
		}		
		return super.visit(arrExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsRecExpr recExpr, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			// First perform a name check for the component name
			SemRecType recType = (SemRecType) recExpr.record.accept(this, visArg);
			SymbTable table = symbTables.get(recType);
			
			AbsDecl compDecl = null;
			try {
				compDecl = table.fnd(recExpr.comp.name);	
				SemAn.declaredAt.put(recExpr.comp, compDecl);
			} catch (CannotFndNameException e) {
				throw new Report.Error(recExpr.location(), "[nameResolution] Record does not cotain compoment " + recExpr.comp.name);
			}
			
			// Then connect the type
			SemType thisType = compDecl.accept(this, visArg);
			SemAn.ofType.put(recExpr, thisType);
			return thisType; 
		} 
		return null;
	}
	
	@Override
	public SemType visit(AbsFunName funName, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			AbsDecl decl = SemAn.declaredAt.get(funName);
			if(!(decl instanceof AbsFunDecl)) {
				throw new Report.Error(funName.location() , "[typeResolving] Not a function.");
			}
			AbsFunDecl funDecl = (AbsFunDecl) decl;
			
			Vector<AbsExpr> args = funName.args.args();
			Vector<AbsParDecl> pars = funDecl.parDecls.parDecls();
			if(args.size() != pars.size()) {
				throw new Report.Error(funName.location(), "[typeResolving] Number of function parameters does not match.");
			}
			
			for (int i = 0; i < args.size(); i++) {
				AbsExpr arg = args.get(i);
				AbsParDecl par = pars.get(i);
				
				SemType argType = arg.accept(this, visArg);
				SemType parType = par.accept(this, visArg);
				
				if(!( (argType instanceof SemBoolType && parType instanceof SemBoolType)
						|| (argType instanceof SemCharType && parType instanceof SemCharType)
						|| (argType instanceof SemIntType && parType instanceof SemIntType)
						|| (argType instanceof SemPtrType && parType instanceof SemPtrType && ((SemPtrType)argType).matches((SemPtrType) parType)))) {
					throw new Report.Error(funName.location(), "[typeResolving] Parameter or argument type not allowed or does not match function definition.");
				}
				
				if(!argType.matches(parType)) {
					throw new Report.Error(funName.location(), "[typeResolving] Parameter types do not match.");
				}
			}
			
			// Return type
			SemType returnType = funDecl.type.accept(this, visArg);
			if(!( returnType instanceof SemVoidType
					|| returnType instanceof SemBoolType
					|| returnType instanceof SemCharType 
					|| returnType instanceof SemIntType
					|| returnType instanceof SemPtrType )) {
				throw new Report.Error(funName.location(), "[typeResolving] Return type of function not allowed.");
			}
			
			SemAn.ofType.put(funName, returnType);
			return returnType; 
		}
		return super.visit(funName, visArg);
	}
	
	@Override
	public SemType visit(AbsBlockExpr blockExpr, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType type = blockExpr.expr.accept(this, visArg);
			SemAn.ofType.put(blockExpr, type);
			
			// Make sure to still visit all the other branches before returning
			blockExpr.decls.accept(this, visArg);
			blockExpr.stmts.accept(this, visArg);
			
			return type;
		}
		
		return super.visit(blockExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsCastExpr castExpr, Phase visArg) {

		if(visArg == Phase.EXPR_LINK) {
			SemType eType = castExpr.expr.accept(this, visArg);
			SemType tType = castExpr.type.accept(this, visArg);
			if(!(eType instanceof SemCharType 
					|| eType instanceof SemIntType
					|| eType instanceof SemPtrType )) {
				throw new Report.Error(castExpr.location(), "[typeResolving] Expression type in cast not allowed.");
			}
			if(!(tType instanceof SemCharType 
					|| tType instanceof SemIntType
					|| tType instanceof SemPtrType )) {
				throw new Report.Error(castExpr.location(), "[typeResolving] Cast to specified type not allowed.");
			}
			
			SemAn.ofType.put(castExpr, tType);
			return tType;
		}
		return super.visit(castExpr, visArg);
	}
	
	@Override
	public SemType visit(AbsAssignStmt assignStmt, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType dstType = assignStmt.dst.accept(this, visArg);
			SemType srcType = assignStmt.src.accept(this, visArg);
			if(!dstType.matches(srcType)) {
				throw new Report.Error(assignStmt.location(), "[typeResolving] Assign statement types don't match.");
			}
			if(!(srcType instanceof SemCharType 
					|| srcType instanceof SemCharType 
					|| srcType instanceof SemIntType
					|| srcType instanceof SemPtrType )) {
				throw new Report.Error(assignStmt.location(), "[typeResolving] Type not allowed in assignment.");
			}

			return new SemVoidType();
		}
		
		return super.visit(assignStmt, visArg);
	}
	
	@Override
	public SemType visit(AbsStmts stmts, Phase visArg) {
		super.visit(stmts, visArg);
		return new SemVoidType();
	}
	
	@Override
	public SemType visit(AbsIfStmt ifStmt, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType condType = ifStmt.cond.accept(this, visArg);
			SemType thenType = ifStmt.thenStmts.accept(this, visArg);
			SemType elseType = null;
			if(ifStmt.elseStmts != null) {
				 elseType = ifStmt.elseStmts.accept(this, visArg);
			}
			
			if(!(condType instanceof SemBoolType)) {
				throw new Report.Error(ifStmt.location(), "[typeResolving] Condition must be of type bool.");
			}		
			if(!(thenType instanceof SemVoidType)) {
				throw new Report.Error(ifStmt.location(), "[typeResolving] Then statements must be of type void.");
			}
			if(elseType != null) {
				if(!(elseType instanceof SemVoidType)) {
					throw new Report.Error(ifStmt.location(), "[typeResolving] Else statements must be of type void.");
				}
			}

			return new SemVoidType();
			
		}
		return super.visit(ifStmt, visArg);
	}
	
	@Override
	public SemType visit(AbsWhileStmt whileStmt, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType condType = whileStmt.cond.accept(this, visArg);
			SemType doType = whileStmt.stmts.accept(this, visArg);
			
			if(!(condType instanceof SemBoolType)) {
				throw new Report.Error(whileStmt.location(), "[typeResolving] Condition must be of type bool.");
			}		
			if(!(doType instanceof SemVoidType)) {
				throw new Report.Error(whileStmt.location(), "[typeResolving] Do statements must be of type void.");
			}
			return new SemVoidType();
			
		}
		return super.visit(whileStmt, visArg);
	}

	@Override
	public SemType visit(AbsFunDef funDef, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			// Just check if everything is ok
			
			Vector<AbsParDecl> pars = funDef.parDecls.parDecls();

			for (int i = 0; i < pars.size(); i++) {
				AbsParDecl par = pars.get(i);
				
				SemType parType = par.accept(this, visArg);
				
				if(!( parType instanceof SemBoolType
						|| parType instanceof SemCharType 
						|| parType instanceof SemIntType
						|| parType instanceof SemPtrType )) {
					throw new Report.Error(funDef.location(), "[typeResolving] Parameter type not allowed.");
				}
			}
			
			// Return type
			SemType returnType = funDef.type.accept(this, visArg);
			if(!( returnType instanceof SemVoidType
					|| returnType instanceof SemBoolType
					|| returnType instanceof SemCharType 
					|| returnType instanceof SemIntType
					|| returnType instanceof SemPtrType )) {
				throw new Report.Error(funDef.location(), "[typeResolving] Return type of function not allowed.");
			}
			
			SemType exprType = funDef.value.accept(this, visArg);
			if(!exprType.matches(returnType)) {
				throw new Report.Error(funDef.location(), "[typeResolving] Function body type does not match return type.");
			}
			
			return null; 
		}
		return super.visit(funDef, visArg);
	}
	
	@Override
	public SemType visit(AbsFunDecl funDef, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			// Just check if everything is ok
			
			Vector<AbsParDecl> pars = funDef.parDecls.parDecls();

			for (int i = 0; i < pars.size(); i++) {
				AbsParDecl par = pars.get(i);
				
				SemType parType = par.accept(this, visArg);
				
				if(!( parType instanceof SemBoolType
						|| parType instanceof SemCharType 
						|| parType instanceof SemIntType
						|| parType instanceof SemPtrType )) {
					throw new Report.Error(funDef.location(), "[typeResolving] Parameter type not allowed.");
				}
			}
			
			// Return type
			SemType returnType = funDef.type.accept(this, visArg);
			if(!( returnType instanceof SemVoidType
					|| returnType instanceof SemBoolType
					|| returnType instanceof SemCharType 
					|| returnType instanceof SemIntType
					|| returnType instanceof SemPtrType )) {
				throw new Report.Error(funDef.location(), "[typeResolving] Return type of function not allowed.");
			}
						
			return null; 
		}
		return super.visit(funDef, visArg);
	}
	
}
