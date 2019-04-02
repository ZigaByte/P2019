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
		DECLARES_TYPE_CREATION, DECLARES_TYPE_LINKING
	}
	
	/** Symbol tables of individual record types. */
	private final HashMap<SemRecType, SymbTable> symbTables = new HashMap<SemRecType, SymbTable>();

	@Override
	public SemType visit(AbsSource source, Phase visArg) {
		super.visit(source, Phase.DECLARES_TYPE_CREATION);
		
		System.out.println(SemAn.declaresType);
		
		super.visit(source, Phase.DECLARES_TYPE_LINKING);
		
		System.out.println(SemAn.declaresType);
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
			switch (atomType.type) {
			case INT:
				return new SemIntType();
			case BOOL:
				return new SemBoolType();
			case CHAR:
				return new SemCharType();
			case VOID:
				return new SemVoidType();
			}
		}
		return null;
	}
	
	@Override
	public SemType visit(AbsArrType arrType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			// TODO WHAT about constants as variables?????
			if(arrType.len instanceof AbsAtomExpr) {
				return new SemArrType(Integer.parseInt(((AbsAtomExpr)arrType.len).expr), (SemType)arrType.elemType.accept(this, visArg));
			}
			System.out.println("IDK, ERROR, NOT AN INTERGER CONSTANT IN THE ARRAY");
		}
		return null;
	}
	
	@Override
	public SemType visit(AbsPtrType ptrType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			return new SemPtrType(ptrType.ptdType.accept(this, visArg));
		}
		return null;
	}
	
	
	@Override
	public SemType visit(AbsRecType recType, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_CREATION) {
			// add to symbol array
		}
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			Vector<SemType> compTypes = new Vector<>();
			for(AbsCompDecl decl : recType.compDecls.compDecls()) {
				compTypes.add(decl.accept(this, Phase.DECLARES_TYPE_LINKING));
				System.out.println(decl.accept(this, Phase.DECLARES_TYPE_LINKING));
			}
			return new SemRecType(compTypes);
		}
		return null;
	}
	
	@Override
	public SemType visit(AbsCompDecl compDecl, Phase visArg) {
		return compDecl.type.accept(this, visArg);
	}
	
	@Override
	public SemType visit(AbsTypName typName, Phase visArg) {
		if(visArg == Phase.DECLARES_TYPE_LINKING) {
			return (SemType) SemAn.declaredAt.get(typName).type.accept(this, visArg);
		}
		return super.visit(typName, visArg);
	}
}
