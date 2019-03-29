/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.common.report.Report.Error;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.phases.seman.SymbTable.CannotFndNameException;
import compiler.phases.seman.SymbTable.CannotInsNameException;

/**
 * Name resolving: the result is stored in {@link SemAn#declaredAt}.
 * 
 * @author sliva
 */
public class NameResolver extends AbsFullVisitor<Object, Object> {

	/** Symbol table. */
	private final SymbTable symbTable = new SymbTable();
	
	@Override
	public Object visit(AbsTypDecl typDecl, Object visArg) {
		try {
			symbTable.ins(typDecl.name, typDecl);
		} catch(CannotInsNameException e) {
			throw new Report.Error("[nameResolution] " + typDecl.location() + " Could not insert " + typDecl.name);
		}
		return super.visit(typDecl, visArg);
	}
	
	@Override
	public Object visit(AbsFunDef funDef, Object visArg) {
		try {
			symbTable.ins(funDef.name, funDef);
		} catch(CannotInsNameException e) {
			throw new Report.Error("[nameResolution] " + funDef.location() + " Could not insert " + funDef.name);
		}
		return super.visit(funDef, visArg);
	}
	
	@Override
	public Object visit(AbsFunDecl funDecl, Object visArg) {
		try {
			symbTable.ins(funDecl.name, funDecl);
		} catch(CannotInsNameException e) {
			throw new Report.Error("[nameResolution] " + funDecl.location() + " Could not insert " + funDecl.name);
		}
		return super.visit(funDecl, visArg);
	}
	
	@Override
	public Object visit(AbsVarDecl varDecl, Object visArg) {
		try {
			symbTable.ins(varDecl.name, varDecl);
		} catch(CannotInsNameException e) {
			throw new Report.Error("[nameResolution] " + varDecl.location() + " Could not insert " + varDecl.name);
		}
		return super.visit(varDecl, visArg);
	}
	
	@Override
	public Object visit(AbsParDecl parDecl, Object visArg) {
		//symbTable.newScope();
		// TODO Auto-generated method stub
		return super.visit(parDecl, visArg);
	}
	
	@Override
	public Object visit(AbsFunName funName, Object visArg) {
		// TODO Auto-generated method stub
		return super.visit(funName, visArg);
	}
	

	
	@Override
	public Object visit(AbsTypName varDecl, Object visArg) {
		System.out.println("hell2o");
		try {
			SemAn.declaredAt.put(varDecl, symbTable.fnd(varDecl.name));
		} catch(CannotFndNameException e) {
			throw new Report.Error("Test name ");
		}
		return super.visit(varDecl, visArg);
	}

}
