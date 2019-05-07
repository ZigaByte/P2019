/**
 * @author sliva
 */
package compiler.phases.asmcode;

import java.util.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;
import compiler.data.layout.*;
import compiler.data.asmcode.*;
import compiler.common.report.*;

/**
 * @author sliva
 */
public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

	@Override
	public Vector<AsmInstr> visit(ImcLABEL label, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();
		toReturn.add(new AsmLABEL(label.label));
		return toReturn;
	}
	
	@Override
	public Vector<AsmInstr> visit(ImcMOVE move, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();

		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();
		
		ImcExpr leftExpr = move.dst;
		if(leftExpr instanceof ImcMEM) {
			ImcMEM memExpr = (ImcMEM) move.dst;

			// Resolve content
			uses.add(move.src.accept(new ExprGenerator(), toReturn));
			// Resolve address
			uses.add(memExpr.addr.accept(new ExprGenerator(), toReturn));
			
			toReturn.add(new AsmOPER("STO `s0, `s1, 0", uses, null, null));	
		} else {
			// Else move into temp			
			defs.add(move.dst.accept(new ExprGenerator(), toReturn));
			uses.add(move.src.accept(new ExprGenerator(), toReturn));
			
			toReturn.add(new AsmOPER("SET `d0, `s0", uses, defs, null));
		}
		
		return toReturn;
	}
	
	public Vector<AsmInstr> visit(ImcJUMP jump, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();

		Vector<Label> jumps = new Vector<>();
		jumps.add(jump.label);	
		
		Vector<Temp> defs = new Vector<>();
		defs.add(new Temp());
		
		toReturn.add(new AsmOPER("GETA `d0, " + jump.label.name, null, defs, null));
		toReturn.add(new AsmOPER("JMP `s0", defs,  null, jumps));

		return toReturn;
	}
	
	@Override
	public Vector<AsmInstr> visit(ImcCJUMP cjump, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();

		Label toJumpTo = cjump.posLabel;
		
		Vector<Label> jumps = new Vector<>();
		jumps.add(toJumpTo);	
		
		Vector<Temp> defs = new Vector<>();
		defs.add(new Temp());
		
		toReturn.add(new AsmOPER("GETA `d0, " + toJumpTo.name, null, defs, null));

		Vector<Temp> uses = new Vector<>();
		uses.add(cjump.cond.accept(new ExprGenerator(), toReturn));
		uses.addAll(defs);
		toReturn.add(new AsmOPER("BNZ `s0, `s1", uses,  null, jumps));
		
		return toReturn;
	}
	
	@Override
	public Vector<AsmInstr> visit(ImcESTMT eStmt, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();
		eStmt.expr.accept(new ExprGenerator(), toReturn);
		return toReturn;
	}
	
}



