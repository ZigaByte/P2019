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
		return super.visit(atomExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsUnExpr unExpr, Object visArg) {
		if(unExpr.oper == Oper.ADDR) {
			SemAn.isAddr.put(unExpr, true);	
		} else {
			SemAn.isAddr.put(unExpr, false);	
		}
		return super.visit(unExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsBinExpr binExpr, Object visArg) {
		SemAn.isAddr.put(binExpr, false);
		return super.visit(binExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsArrExpr arrExpr, Object visArg) {
		Boolean isAddr = arrExpr.array.accept(this, visArg);
		SemAn.isAddr.put(arrExpr, isAddr);
		
		return SemAn.isAddr.get(arrExpr);
	}
	
	@Override
	public Boolean visit(AbsBlockExpr blockExpr, Object visArg) {
		SemAn.isAddr.put(blockExpr, false);
		return super.visit(blockExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsCastExpr castExpr, Object visArg) {
		SemAn.isAddr.put(castExpr, false);
		return super.visit(castExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsDelExpr delExpr, Object visArg) {
		SemAn.isAddr.put(delExpr, false);
		return super.visit(delExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsNewExpr newExpr, Object visArg) {
		SemAn.isAddr.put(newExpr, false);
		return super.visit(newExpr, visArg);
	}
	
	@Override
	public Boolean visit(AbsRecExpr recExpr, Object visArg) {
		Boolean isAddr = recExpr.record.accept(this, visArg);
		SemAn.isAddr.put(recExpr, isAddr);
		
		return SemAn.isAddr.get(recExpr);
	}
	
	@Override
	public Boolean visit(AbsVarName varName, Object visArg) {
		SemAn.isAddr.put(varName, true);
		return super.visit(varName, visArg);
	}
	
	@Override
	public Boolean visit(AbsAssignStmt assignStmt, Object visArg) {
		// TODO: Perform actual check with assinment statements
		assignStmt.src.accept(this, visArg);
		assignStmt.dst.accept(this, visArg);
		
		if(!SemAn.isAddr.get(assignStmt.dst)) {
			throw new Report.Error(assignStmt.location(), "[AddrResolver] LValue is not an address!");
		}
		
		return null;
	}
	
	
}
