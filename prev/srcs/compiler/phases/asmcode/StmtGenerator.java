/**
 * @author sliva
 */
package compiler.phases.asmcode;

import java.util.Vector;

import compiler.data.asmcode.AsmInstr;
import compiler.data.asmcode.AsmLABEL;
import compiler.data.asmcode.AsmMOVE;
import compiler.data.asmcode.AsmOPER;
import compiler.data.imcode.ImcCJUMP;
import compiler.data.imcode.ImcESTMT;
import compiler.data.imcode.ImcExpr;
import compiler.data.imcode.ImcJUMP;
import compiler.data.imcode.ImcLABEL;
import compiler.data.imcode.ImcMEM;
import compiler.data.imcode.ImcMOVE;
import compiler.data.imcode.ImcTEMP;
import compiler.data.imcode.visitor.ImcVisitor;
import compiler.data.layout.Label;
import compiler.data.layout.Temp;

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
			
			toReturn.add(new AsmOPER("STO `s0,`s1,0", uses, null, null));	
		} else {
			// Else move into temp

			
			if(move.src instanceof ImcTEMP && move.dst instanceof ImcTEMP) {
				defs.add(move.dst.accept(new ExprGenerator(), toReturn));
				uses.add(move.src.accept(new ExprGenerator(), toReturn));
				toReturn.add(new AsmMOVE("SET `d0,`s0", uses, defs));
			}else {
				Temp temp = move.dst.accept(new ExprGenerator(), toReturn);
				uses.add(move.src.accept(new ExprGenerator(temp), toReturn));
			}
		}
		
		return toReturn;
	}
	
	public Vector<AsmInstr> visit(ImcJUMP jump, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();

		Vector<Label> jumps = new Vector<>();
		jumps.add(jump.label);	
		
		toReturn.add(new AsmOPER("JMP " + jump.label.name, null,  null, jumps));

		return toReturn;
	}
	
	@Override
	public Vector<AsmInstr> visit(ImcCJUMP cjump, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();

		Label toJumpTo = cjump.posLabel;
		
		Vector<Label> jumps = new Vector<>();
		jumps.add(toJumpTo);	
		
		Vector<Temp> uses = new Vector<>();
		uses.add(cjump.cond.accept(new ExprGenerator(), toReturn));
		toReturn.add(new AsmOPER("BNZ `s0," + toJumpTo.name, uses,  null, jumps));
		
		return toReturn;
	}
	
	@Override
	public Vector<AsmInstr> visit(ImcESTMT eStmt, Object visArg) {
		Vector<AsmInstr> toReturn = new Vector<AsmInstr>();
		eStmt.expr.accept(new ExprGenerator(), toReturn);
		return toReturn;
	}
	
}



