/**
 * @author sliva
 */
package compiler.phases.imcgen;

import java.util.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.imcode.ImcBINOP;
import compiler.data.imcode.ImcCONST;
import compiler.data.imcode.ImcExpr;
import compiler.data.imcode.ImcUNOP;
import compiler.data.imcode.ImcUNOP.Oper;
import compiler.data.imcode.visitor.ImcVisitor;
import compiler.data.layout.*;
import compiler.phases.frames.*;

/**
 * Intermediate code generator.
 * 
 * This is a plain full visitor
 * 
 * @author sliva
 */
public class CodeGenerator extends AbsFullVisitor<Object, Stack<Frame>> {

	@Override
	public Object visit(AbsAtomExpr atomExpr, Stack<Frame> visArg) {
		switch (atomExpr.type) {
		case VOID:
		case PTR: // none or null
			ImcGen.exprImCode.put(atomExpr, new ImcCONST(0));
			break;
		case BOOL:
			ImcGen.exprImCode.put(atomExpr, new ImcCONST(atomExpr.expr.equals("true")?1:0));
			break;
		case CHAR:
			ImcGen.exprImCode.put(atomExpr, new ImcCONST(atomExpr.expr.charAt(1)));
			break;
		case INT:
			ImcGen.exprImCode.put(atomExpr, new ImcCONST(Long.parseLong(atomExpr.expr)));
			break;
			
		case STR:
			// TODO
			break;
		}
		return ImcGen.exprImCode.get(atomExpr);
	}
	
	@Override
	public Object visit(AbsUnExpr unExpr, Stack<Frame> visArg) {
		ImcExpr subExpr = (ImcExpr) unExpr.subExpr.accept(this, visArg);
		
		ImcUNOP.Oper oper = null;
		switch (unExpr.oper) {
		case SUB:
			oper = ImcUNOP.Oper.NEG;
			break;
		case NOT:
			oper = ImcUNOP.Oper.NOT;
			break;
		default:
			// Nothing to do really with other prefixes
			break;
		}
		
		if(oper != null) {
			ImcGen.exprImCode.put(unExpr, new ImcUNOP(oper, subExpr));
		}
		
		return ImcGen.exprImCode.get(unExpr);
	}

	@Override
	public Object visit(AbsBinExpr binExpr, Stack<Frame> visArg) {
		ImcExpr expr1 = (ImcExpr) binExpr.fstExpr.accept(this, visArg);
		ImcExpr expr2 = (ImcExpr) binExpr.sndExpr.accept(this, visArg);
		
		HashMap<AbsBinExpr.Oper, ImcBINOP.Oper> operations = new HashMap<>();
		operations.put(AbsBinExpr.Oper.IOR, ImcBINOP.Oper.IOR);
		operations.put(AbsBinExpr.Oper.XOR, ImcBINOP.Oper.XOR);
		operations.put(AbsBinExpr.Oper.AND, ImcBINOP.Oper.AND);
		operations.put(AbsBinExpr.Oper.EQU, ImcBINOP.Oper.EQU);
		operations.put(AbsBinExpr.Oper.NEQ, ImcBINOP.Oper.NEQ);
		operations.put(AbsBinExpr.Oper.GEQ, ImcBINOP.Oper.GEQ);
		operations.put(AbsBinExpr.Oper.LEQ, ImcBINOP.Oper.LEQ);
		operations.put(AbsBinExpr.Oper.GTH, ImcBINOP.Oper.GTH);
		operations.put(AbsBinExpr.Oper.LTH, ImcBINOP.Oper.LTH);
		operations.put(AbsBinExpr.Oper.ADD, ImcBINOP.Oper.ADD);
		operations.put(AbsBinExpr.Oper.SUB, ImcBINOP.Oper.SUB);
		operations.put(AbsBinExpr.Oper.MUL, ImcBINOP.Oper.MUL);
		operations.put(AbsBinExpr.Oper.DIV, ImcBINOP.Oper.DIV);
		operations.put(AbsBinExpr.Oper.MOD, ImcBINOP.Oper.MOD);
		
		ImcBINOP.Oper oper = operations.get(binExpr.oper);
		if(oper != null) {
			ImcGen.exprImCode.put(binExpr, new ImcBINOP(oper, expr1, expr2));
		}
		
		return ImcGen.exprImCode.get(binExpr);
	}
	
}
