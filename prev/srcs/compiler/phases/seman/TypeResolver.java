/**
 * @author sliva 
 */
package compiler.phases.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.AbsBinExpr.Oper;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.type.property.*;

/**
 * Type resolving: the result is stored in {@link SemAn#declaresType},
 * {@link SemAn#isType}, and {@link SemAn#ofType}.
 * 
 * @author sliva
 */
public class TypeResolver extends AbsFullVisitor<SemType, TypeResolver.Phase> {

	enum Phase {
		DECLARES_TYPE_CREATION, DECLARES_TYPE_LINKING, EXPR_LINK, TYPE_RESOLUTION
	}

	/** Symbol tables of individual record types. */
	private final HashMap<SemRecType, SymbTable> symbTables = new HashMap<SemRecType, SymbTable>();

	@Override
	public SemType visit(AbsSource source, Phase visArg) {
		super.visit(source, Phase.DECLARES_TYPE_CREATION);

		// System.out.println(SemAn.declaresType);
		// System.out.println(SemAn.isType);

		super.visit(source, Phase.DECLARES_TYPE_LINKING);
		super.visit(source, Phase.EXPR_LINK);

		// System.out.println(SemAn.declaresType);
		// System.out.println(SemAn.isType);

		return null;
	}

	@Override
	public SemType visit(AbsTypDecl typDecl, Phase visArg) {
		if (visArg == Phase.DECLARES_TYPE_CREATION) {
			SemAn.declaresType.put(typDecl, new SemNamedType(typDecl.name));
		}
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			SemNamedType namedType = SemAn.declaresType.get(typDecl);
			namedType.define(typDecl.type.accept(this, Phase.DECLARES_TYPE_LINKING));
		}

		return super.visit(typDecl, visArg);
	}
	
	@Override
	public SemType visit(AbsVarDecl varDecl, Phase visArg) {
		if(visArg == Phase.EXPR_LINK) {
			SemType newType = varDecl.type.accept(this, visArg);
			if(newType instanceof SemVoidType) {
				throw new Report.Error(varDecl.location(), "[typeResolving] Variable may not be of type void");
			}
			// Todo: insert???
			//SemAn.ofType.put(varDecl.name, newType);
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
			// TODO WHAT about constants as variables?????
			if (arrType.len instanceof AbsAtomExpr) {
				int len = Integer.parseInt(((AbsAtomExpr) arrType.len).expr);
				SemType elementType = arrType.elemType.accept(this, visArg);
				if (elementType instanceof SemVoidType) {
					throw new Report.Error(arrType, String.format("[typeResolving] Void type not allowed in array."));
				}
				SemType newType = new SemArrType(len, elementType);
				SemAn.isType.put(arrType, newType);
				return newType;
			}
			System.out.println("IDK, ERROR, NOT AN INTERGER CONSTANT IN THE ARRAY");
		}else if(visArg == Phase.EXPR_LINK){
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
		if (visArg == Phase.DECLARES_TYPE_CREATION) {
			// add to symbol array
		}
		if (visArg == Phase.DECLARES_TYPE_LINKING) {
			Vector<SemType> compTypes = new Vector<>();
			for (AbsCompDecl decl : recType.compDecls.compDecls()) {
				SemType elementType = decl.accept(this, Phase.DECLARES_TYPE_LINKING);
				if (elementType instanceof SemVoidType) {
					throw new Report.Error(recType, String.format("[typeResolving] Void type not allowed in record."));
				}
				compTypes.add(elementType);
			}
			SemType newType = new SemRecType(compTypes);
			SemAn.isType.put(recType, newType);
			return newType;
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
		} else {
			return super.visit(atomExpr, visArg);
		}

	}

	@Override
	public SemType visit(AbsUnExpr unExpr, Phase visArg) {
		if (visArg == Phase.EXPR_LINK) {
			switch (unExpr.oper) {
			case NOT:{
				SemType insideType = unExpr.subExpr.accept(this, visArg);
				if(!(insideType instanceof SemBoolType)) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression next to ! must be of type bool."));
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			case SUB:
			case ADD:{
				SemType insideType = unExpr.subExpr.accept(this, visArg);
				if(!(insideType instanceof SemIntType)) {
					throw new Report.Error(unExpr.location(), String.format("[typeResolving] Expression next to unary +,- must be of type int."));
				}
				SemType thisType = new SemIntType();
				SemAn.ofType.put(unExpr, thisType);
				return thisType; 
			}
			}
			
			return null;
		} else {
			return super.visit(unExpr, visArg);
		}
	}
	
	@Override
	public SemType visit(AbsBinExpr binExpr, Phase visArg) {
		if (visArg == Phase.EXPR_LINK) {
			switch (binExpr.oper) {
			case AND:
			case IOR:
			case XOR:{
				SemType insideType1 = binExpr.fstExpr.accept(this, visArg);
				SemType insideType2 = binExpr.sndExpr.accept(this, visArg);
				if(!(insideType1 instanceof SemBoolType) || !(insideType2 instanceof SemBoolType)) {
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
				// TODO Check if the types have to be the same.
				SemType it1 = binExpr.fstExpr.accept(this, visArg);
				SemType it2 = binExpr.sndExpr.accept(this, visArg);
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
				SemType it1 = binExpr.fstExpr.accept(this, visArg);
				SemType it2 = binExpr.sndExpr.accept(this, visArg);
				if(!( (it1 instanceof SemBoolType && it2 instanceof SemBoolType)
						|| (it1 instanceof SemCharType && it2 instanceof SemCharType)
						|| (it1 instanceof SemIntType && it2 instanceof SemIntType)
						|| (it1 instanceof SemPtrType && it2 instanceof SemPtrType))) {
					throw new Report.Error(binExpr.location(), "[typeResolving] Expressions around == or != must be of type int, char, bool or ptr. The two types must be the same.");
				}
				SemType thisType = new SemBoolType();
				SemAn.ofType.put(binExpr, thisType);
				return thisType; 
			}
							
			}
			
			return null;
		} else {
			return super.visit(binExpr, visArg);
		}	
	}
}
