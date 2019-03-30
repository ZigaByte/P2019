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

	private enum Mode {
		NAME_FIND, NAME_RESOLVE, ADD_FUN_PARAMS
	}

	/** Symbol table. */
	private final SymbTable symbTable = new SymbTable();

	@Override
	public Object visit(AbsDecls decls, Object visArg) {
		// 1st pass NAME_FIND
		super.visit(decls, Mode.NAME_FIND);

		// 2nd pass NAME_RESOLVE
		super.visit(decls, Mode.NAME_RESOLVE);

		return null;
	}

	@Override
	public Object visit(AbsTypDecl typDecl, Object visArg) {
		if (visArg == Mode.NAME_FIND) {
			try {				
				symbTable.ins(typDecl.name, typDecl);
			} catch (CannotInsNameException e) {
				throw new Report.Error("[nameResolution] " + typDecl.location() + " Could not insert " + typDecl.name);
			}
			return null;
		} else {
			return super.visit(typDecl, visArg);
		}
	}

	@Override
	public Object visit(AbsVarDecl varDecl, Object visArg) {
		if (visArg == Mode.NAME_FIND) {
			try {
				symbTable.ins(varDecl.name, varDecl);
			} catch (CannotInsNameException e) {
				throw new Report.Error("[nameResolution] " + varDecl.location() + " Could not insert " + varDecl.name);
			}
			return null;
		} else {
			return super.visit(varDecl, visArg);
		}
	}

	@Override
	public Object visit(AbsTypName typDecl, Object visArg) {
		try {
			AbsDecl decl = symbTable.fnd(typDecl.name);
			if (decl instanceof AbsTypDecl) {
				SemAn.declaredAt.put(typDecl, decl);
			} else {
				throw new Report.Error(typDecl.location(), String.format("[nameResolution] Type name %s is not a type.", typDecl.name));
			}
		} catch (CannotFndNameException e) {
			throw new Report.Error(typDecl.location(), String.format("[nameResolution] Type name %s not found.", typDecl.name));
		}
		return super.visit(typDecl, visArg);
	}

	@Override
	public Object visit(AbsVarName varDecl, Object visArg) {
		try {
			AbsDecl decl = symbTable.fnd(varDecl.name);
			if (decl instanceof AbsVarDecl || decl instanceof AbsParDecl) {
				SemAn.declaredAt.put(varDecl, decl);
			} else {
				throw new Report.Error(varDecl.location(), String.format("[nameResolution] Variable name %s is not a variable or parameter.", varDecl.name));
			}
		} catch (CannotFndNameException e) {
			throw new Report.Error(varDecl.location(), String.format("[nameResolution] Variable name %s not found.", varDecl.name));
		}
		return super.visit(varDecl, visArg);
	}

	@Override
	// fun test (a:int) : char;
	public Object visit(AbsFunDecl funDecl, Object visArg) {
		if (visArg == Mode.NAME_FIND) {
			try {
				symbTable.ins(funDecl.name, funDecl);
			} catch (CannotInsNameException e) {
				throw new Report.Error("[nameResolution] " + funDecl.location() + " Could not insert " + funDecl.name);
			}
			return null;
		} else {
			return super.visit(funDecl, visArg);
		}
	}

	@Override
	// fun test (a:int) : char = expr;
	public Object visit(AbsFunDef funDef, Object visArg) {
		if (visArg == Mode.NAME_FIND) {
			try {
				symbTable.ins(funDef.name, funDef);
			} catch (CannotInsNameException e) {
				throw new Report.Error("[nameResolution] " + funDef.location() + " Could not insert " + funDef.name);
			}
			return null;
		} else {
			// Find the type
			funDef.type.accept(this, visArg);

			// Create new scope
			symbTable.newScope();

			// Somehow add parameters
			funDef.parDecls.accept(this, Mode.ADD_FUN_PARAMS);

			funDef.value.accept(this, Mode.NAME_FIND);

			symbTable.oldScope();

			return null;
		}
	}
	
	@Override
	public Object visit(AbsFunName funName, Object visArg) {
		try {
			AbsDecl decl = symbTable.fnd(funName.name);
			if (decl instanceof AbsFunDecl || decl instanceof AbsFunDef) {
				SemAn.declaredAt.put(funName, decl);
			} else {
				throw new Report.Error(funName.location(), String.format("[nameResolution] Variable name %s is not a variable or parameter.", funName.name));
			}
		} catch (CannotFndNameException e) {
			throw new Report.Error(funName.location(), String.format("[nameResolution] Variable name %s not found.", funName.name));
		}
		return super.visit(funName, visArg);
	}

	@Override
	public Object visit(AbsParDecl parDecl, Object visArg) {
		if (visArg == Mode.ADD_FUN_PARAMS) {
			try {
				symbTable.ins(parDecl.name, parDecl);
			} catch (CannotInsNameException e) {
				throw new Report.Error("[nameResolution] " + parDecl.location() + " Could not insert " + parDecl.name);
			}
		}
		return super.visit(parDecl, visArg);
	}

}
