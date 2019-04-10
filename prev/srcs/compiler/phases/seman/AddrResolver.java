/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.AbsUnExpr.Oper;
import compiler.data.abstree.visitor.*;

/**
 * Determines which value expression can denote an address.
 * 
 * @author sliva
 */
public class AddrResolver extends AbsFullVisitor<Boolean, Object> {

	@Override
	public Boolean visit(AbsAtomExpr atomExpr, Object visArg) {
		SemAn.isAddr.put(atomExpr, false);
		super.visit(atomExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsUnExpr unExpr, Object visArg) {
		if(unExpr.oper == Oper.DATA) {
			SemAn.isAddr.put(unExpr, true);	
			super.visit(unExpr, visArg);
			return true;
		} else {
			SemAn.isAddr.put(unExpr, false);	
			super.visit(unExpr, visArg);
			return false;
		}
	}
	
	@Override
	public Boolean visit(AbsBinExpr binExpr, Object visArg) {
		SemAn.isAddr.put(binExpr, false);
		super.visit(binExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsArrExpr arrExpr, Object visArg) {
		Boolean isAddr = arrExpr.array.accept(this, visArg);
		SemAn.isAddr.put(arrExpr, isAddr);
		arrExpr.index.accept(this, visArg);
		
		return SemAn.isAddr.get(arrExpr);
	}
	
	@Override
	public Boolean visit(AbsBlockExpr blockExpr, Object visArg) {
		SemAn.isAddr.put(blockExpr, false);
		super.visit(blockExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsCastExpr castExpr, Object visArg) {
		SemAn.isAddr.put(castExpr, false);
		super.visit(castExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsDelExpr delExpr, Object visArg) {
		SemAn.isAddr.put(delExpr, false);
		super.visit(delExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsNewExpr newExpr, Object visArg) {
		SemAn.isAddr.put(newExpr, false);
		super.visit(newExpr, visArg);
		return false;
	}
	
	@Override
	public Boolean visit(AbsRecExpr recExpr, Object visArg) {
		Boolean isAddr = recExpr.record.accept(this, visArg);
		SemAn.isAddr.put(recExpr, isAddr);
		
		recExpr.comp.accept(this, visArg);
		
		return SemAn.isAddr.get(recExpr);
	}
	
	@Override
	public Boolean visit(AbsVarName varName, Object visArg) {
		SemAn.isAddr.put(varName, true);
		super.visit(varName, visArg);
		return true;
	}
	
	@Override
	public Boolean visit(AbsAssignStmt assignStmt, Object visArg) {
		assignStmt.src.accept(this, visArg);
		assignStmt.dst.accept(this, visArg);
		
		if(!SemAn.isAddr.get(assignStmt.dst)) {
			throw new Report.Error(assignStmt.location(), "[AddrResolver] LValue is not an address!");
		}
		
		return null;
	}
	
	
}
