/**
 * @author sliva 
 */
package compiler.phases.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.data.abstree.*;
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

	enum Phase{
		DECLARES_TYPE_CREATION, DECLARES_TYPE_LINKING,
		TYPE_RESOLUTION
	}
	
	/** Symbol tables of individual record types. */
	private final HashMap<SemRecType, SymbTable> symbTables = new HashMap<SemRecType, SymbTable>();

	@Override
	public SemType visit(AbsSource source, Phase visArg) {
		super.visit(source, Phase.DECLARES_TYPE_CREATION);
		
		//System.out.println(SemAn.declaresType);
		//System.out.println(SemAn.isType);

		super.visit(source, Phase.DECLARES_TYPE_LINKING);
		
		//System.out.println(SemAn.declaresType);
		//System.out.println(SemAn.isType);
		
		return null;
	}
	
	@Override
	public SemType visit(AbsTypDecl typDecl, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_CREATION) {
			SemAn.declaresType.put(typDecl, new SemNamedType(typDecl.name));
		}
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			SemNamedType namedType = SemAn.declaresType.get(typDecl);
			namedType.define(typDecl.type.accept(this, Phase.DECLARES_TYPE_LINKING));			
		}
		
		return super.visit(typDecl, visArg);
	}
	
	@Override
	public SemType visit(AbsAtomType atomType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
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
		}
		return super.visit(atomType, visArg);
	}
	
	@Override
	public SemType visit(AbsArrType arrType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			// TODO WHAT about constants as variables?????
			if(arrType.len instanceof AbsAtomExpr) {
				int len = Integer.parseInt(((AbsAtomExpr)arrType.len).expr);
				SemType elementType = arrType.elemType.accept(this, visArg);
				if(elementType instanceof SemVoidType) {
					throw new Report.Error(arrType, String.format("[typeResolving] Void type not allowed in array."));
				}
				SemType newType = new SemArrType(len, elementType);
				SemAn.isType.put(arrType, newType);
				return newType;
			}
			System.out.println("IDK, ERROR, NOT AN INTERGER CONSTANT IN THE ARRAY");
		}
		return super.visit(arrType, visArg);
	}
	
	@Override
	public SemType visit(AbsPtrType ptrType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			SemType newType = new SemPtrType(ptrType.ptdType.accept(this, visArg));
			SemAn.isType.put(ptrType, newType);
			return newType;
		}
		return super.visit(ptrType, visArg);
	}
	
	
	@Override
	public SemType visit(AbsRecType recType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_CREATION) {
			// add to symbol array
		}
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			Vector<SemType> compTypes = new Vector<>();
			for(AbsCompDecl decl : recType.compDecls.compDecls()) {
				SemType elementType = decl.accept(this, Phase.DECLARES_TYPE_LINKING);
				if(elementType instanceof SemVoidType) {
					throw new Report.Error(recType, String.format("[typeResolving] Void type not allowed in record."));
				}
				compTypes.add(elementType);
			}
			SemType newType = new SemRecType(compTypes);
			SemAn.isType.put(recType, newType);
			return newType;
		}
		return super.visit(recType, visArg);
	}
	
	@Override
	public SemType visit(AbsCompDecl compDecl, Phase visArg) {
		return compDecl.type.accept(this, visArg);
	}
	
	@Override
	public SemType visit(AbsTypName typName, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			SemType newType = SemAn.declaresType.get((AbsTypDecl) SemAn.declaredAt.get(typName));
			SemAn.isType.put(typName, newType);
			return newType;
		}
		return super.visit(typName, visArg);
	}
}
