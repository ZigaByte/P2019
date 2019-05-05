/**
 * @author sliva
 */
package compiler.phases.asmcode;

import java.util.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.imcode.visitor.*;
import compiler.data.asmcode.*;

/**
 * @author sliva
 */
public class ExprGenerator implements ImcVisitor<Temp, Vector<AsmInstr>> {

	@Override
	public Temp visit(ImcCONST constant, Vector<AsmInstr> visArg) {
		Vector<Temp> defs = new Vector<>();
		
		Temp temp = new Temp();
		defs.add(temp);
		
		// TODO Transform constant into binary
		visArg.add(new AsmOPER("SETL `d0, x="+constant.value, null, defs, null));
		visArg.add(new AsmOPER("INCML `d0, x="+constant.value, null, defs, null));
		visArg.add(new AsmOPER("INCMH `d0, x="+constant.value, null, defs, null));
		visArg.add(new AsmOPER("INCH `d0, x="+constant.value, null, defs, null));

		return temp;
	}
	
	@Override
	public Temp visit(ImcTEMP temp, Vector<AsmInstr> visArg) {
		return temp.temp;
	}
	
	@Override
	public Temp visit(ImcBINOP binOp, Vector<AsmInstr> visArg) {
		String instr = "";
		switch (binOp.oper) {
		case ADD:
			instr = "ADD";
			break;
		}

		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();
		
		Temp temp = new Temp();
		defs.add(temp);
		
		uses.add(binOp.fstExpr.accept(this, visArg));
		uses.add(binOp.sndExpr.accept(this, visArg));
		
		visArg.add(new AsmOPER(instr + " `d0, `s0, `s1", uses, defs, null));
		
		return temp;
	}
	
	@Override
	public Temp visit(ImcNAME name, Vector<AsmInstr> visArg) {
		Vector<Temp> uses = new Vector<>();
		uses.add(new Temp());
		
		Vector<Temp> defs = new Vector<>();
		Temp temp = new Temp();
		defs.add(temp);
		
		visArg.add(new AsmOPER("GET `d0, " + name.label.name, null, uses, null));
		
		visArg.add(new AsmOPER("LDO `d0, `s0, 0", uses, defs, null));
		
		return temp;
	}
	
	@Override
	public Temp visit(ImcMEM mem, Vector<AsmInstr> visArg) {

		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();

		Temp temp = new Temp();
		defs.add(temp);		
		uses.add(mem.addr.accept(this, visArg));

		visArg.add(new AsmOPER("LDO `d0, `s0, 0", uses, defs, null));
		
		return temp;
	}
}
