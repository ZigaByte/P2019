/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;

/**
 * @author sliva
 */
public class StmtCanonizer implements ImcVisitor<Vector<ImcStmt>, Object> {	
	
	@Override
	public Vector<ImcStmt> visit(ImcCONST constant, Object visArg) {
		return new Vector<ImcStmt>();
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcTEMP temp, Object visArg) {
		return new Vector<ImcStmt>();
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcNAME temp, Object visArg) {
		return new Vector<ImcStmt>();
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcESTMT eStmt, Object visArg) {
		return eStmt.expr.accept(this, null);
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcLABEL label, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.add(label);
		return toReturn;	
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcJUMP jump, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.add(jump);
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcBINOP binOp, Object visArg) {
		Vector<ImcStmt> fstStmts = binOp.fstExpr.accept(this, null);
		ImcExpr fstExpr = binOp.fstExpr.accept(new ExprCanonizer(), fstStmts);
		Vector<ImcStmt> sndStmts = binOp.sndExpr.accept(this, null);
		ImcExpr sndExpr = binOp.sndExpr.accept(new ExprCanonizer(), sndStmts);

		ImcTEMP t1 = new ImcTEMP(new Temp());
		ImcTEMP t2 = new ImcTEMP(new Temp());
		
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.addAll(fstStmts);
		toReturn.add(new ImcMOVE(t1, fstExpr));
		toReturn.addAll(sndStmts);
		toReturn.add(new ImcMOVE(t2, sndExpr));
		
		// This gets poped by the ExprCanonizer, hopefully
		toReturn.add(new ImcESTMT(new ImcBINOP(binOp.oper, t1, t2)));
		
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcUNOP unOp, Object visArg) {
		Vector<ImcStmt> subStmts = unOp.subExpr.accept(this, null);
		ImcExpr subExpr = unOp.subExpr.accept(new ExprCanonizer(), subStmts);
		
		ImcTEMP t1 = new ImcTEMP(new Temp());
		
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.addAll(subStmts);
		toReturn.add(new ImcMOVE(t1, subExpr));
		
		toReturn.add(new ImcESTMT(new ImcUNOP(unOp.oper, t1)));
		
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcMEM mem, Object visArg) {
		Vector<ImcStmt> addrStmts = mem.addr.accept(this, null);
		ImcExpr addrExpr = mem.addr.accept(new ExprCanonizer(), addrStmts);
		
		ImcTEMP t1 = new ImcTEMP(new Temp());

		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.addAll(addrStmts);
		toReturn.add(new ImcMOVE(t1, addrExpr));
		
		toReturn.add(new ImcESTMT(new ImcMEM(t1)));
		
		return toReturn;
	}
	
	
	@Override
	public Vector<ImcStmt> visit(ImcSEXPR sExpr, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.addAll(sExpr.stmt.accept(this, null));	
		
		Vector<ImcStmt> returnedFromExpr = sExpr.expr.accept(this, null);
		if(returnedFromExpr.size() == 0) {
			toReturn.add(new ImcESTMT(sExpr.expr.accept(new ExprCanonizer(), null)));
		}else {
			toReturn.addAll(returnedFromExpr);	
		}
		ImcESTMT last = (ImcESTMT) toReturn.lastElement();
		toReturn.removeElement(last);
		
		if(visArg instanceof Temp) {
			toReturn.add(new ImcMOVE(new ImcTEMP((Temp)visArg), last.expr));
		}else {
			toReturn.add(new ImcMOVE(new ImcTEMP(new Temp()), last.expr));	
		}
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcSTMTS stmts, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		for(ImcStmt stmt : stmts.stmts()) {
			toReturn.addAll(stmt.accept(this, null));
		}
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcCALL call, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		
		Vector<ImcExpr> newArgs = new Vector<ImcExpr>();
		for(ImcExpr arg: call.args()) {
			ImcExpr newTemp = new ImcTEMP(new Temp());
			newArgs.add(newTemp);
			
			toReturn.addAll(arg.accept(this, null));
			toReturn.add(new ImcMOVE(newTemp, arg.accept(new ExprCanonizer(), toReturn)));
		}
		
		toReturn.add(new ImcESTMT(new ImcCALL(call.label, newArgs)));
		 
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcCJUMP cjump, Object visArg) {
		Vector<ImcStmt> toReturn = new Vector<>();
		
		Vector<ImcStmt> condStmts = cjump.cond.accept(this, null);
		ImcExpr condExpr = cjump.cond.accept(new ExprCanonizer(), condStmts);
		
		ImcTEMP t1 = new ImcTEMP(new Temp());
		toReturn.addAll(condStmts);
		toReturn.add(new ImcMOVE(t1, condExpr));
		
		Label negLabel = new Label();
		toReturn.add(new ImcCJUMP(t1, cjump.posLabel, negLabel));
		toReturn.add(new ImcLABEL(negLabel));
		toReturn.add(new ImcJUMP(cjump.negLabel));		
		
		return toReturn;
	}
	
	@Override
	public Vector<ImcStmt> visit(ImcMOVE move, Object visArg) {
		Vector<ImcStmt> dstStmts = move.dst.accept(this, null);
		ImcExpr dstExpr = move.dst.accept(new ExprCanonizer(), dstStmts);
		Vector<ImcStmt> srcStmts = move.src.accept(this, null);
		ImcExpr srcExpr = move.src.accept(new ExprCanonizer(), srcStmts);
		
		ImcTEMP t1 = new ImcTEMP(new Temp());
		
		Vector<ImcStmt> toReturn = new Vector<>();
		toReturn.addAll(srcStmts);
		toReturn.add(new ImcMOVE(t1, srcExpr));
		toReturn.addAll(dstStmts);
		toReturn.add(new ImcMOVE(dstExpr, t1));
		
		return toReturn;
	}
    
}
