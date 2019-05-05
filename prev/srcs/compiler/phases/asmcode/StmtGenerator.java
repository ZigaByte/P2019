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
			
			// Resolve address
			uses.add(memExpr.addr.accept(new ExprGenerator(), toReturn));
			// Resolve content
			uses.add(move.src.accept(new ExprGenerator(), toReturn));
			
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
		return new Vector<AsmInstr>();
	}
	
}
