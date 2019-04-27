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
public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {
		
	@Override
	public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> visArg) {
		return constant;
	}
	
	@Override
	public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> visArg) {
		return temp;
	}
	
	@Override
	public ImcExpr visit(ImcNAME name, Vector<ImcStmt> visArg) {
		return name;
	}
	
	@Override
	public ImcExpr visit(ImcBINOP binOp, Vector<ImcStmt> visArg) {
		ImcExpr toReturn = ((ImcESTMT)visArg.lastElement()).expr;
		visArg.removeElement(visArg.lastElement());
		return toReturn;
	}
	
	@Override
	public ImcExpr visit(ImcUNOP unOp, Vector<ImcStmt> visArg) {
		ImcExpr toReturn = ((ImcESTMT)visArg.lastElement()).expr;
		visArg.removeElement(visArg.lastElement());
		return toReturn;
	}
	
	@Override
	public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> visArg) {
		ImcExpr toReturn = ((ImcESTMT)visArg.lastElement()).expr;
		visArg.removeElement(visArg.lastElement());
		return toReturn;
	}
	
	@Override
	public ImcExpr visit(ImcSEXPR sExpr, Vector<ImcStmt> visArg) {
		return sExpr.expr;
	}
}
