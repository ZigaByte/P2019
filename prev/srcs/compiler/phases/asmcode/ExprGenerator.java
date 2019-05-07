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
		
		long value = constant.value;
		long l = value & ((1<<16) - 1);
		value >>= 16;
		long ml = value & ((1<<16) - 1);
		value >>= 16;
		long mh = value & ((1<<16) - 1);
		value >>= 16;
		long h = value & ((1<<16) - 1);
		value >>= 16;
		
		visArg.add(new AsmOPER("SETL `d0, " + l, null, defs, null));
		visArg.add(new AsmOPER("INCML `d0, " + ml, null, defs, null));
		visArg.add(new AsmOPER("INCMH `d0, " + mh, null, defs, null));
		visArg.add(new AsmOPER("INCH `d0, " + h, null, defs, null));

		return temp;
	}
	
	@Override
	public Temp visit(ImcTEMP temp, Vector<AsmInstr> visArg) {
		return temp.temp;
	}
	
	@Override
	public Temp visit(ImcBINOP binOp, Vector<AsmInstr> visArg) {
		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();
		
		Temp temp = new Temp();
		defs.add(temp);
		
		uses.add(binOp.fstExpr.accept(this, visArg));
		uses.add(binOp.sndExpr.accept(this, visArg));
		
		
		String instr = "";
		switch (binOp.oper) {
		case ADD:
			instr = "ADD";
			break;
		case SUB:
			instr = "SUB";
			break;
		case MUL:
			instr = "MUL";
			break;
		case DIV:
			instr = "DIV";
			break;
			
		case AND:
			instr = "AND";
			break;
		case IOR:
			instr = "IOR";
			break;
		case XOR:
			instr = "XOR";
			break;
			
		case EQU:
		case GTH:
		case LTH:
		case LEQ:
		case GEQ:
		case NEQ:
			// TODO Compares
			instr = "COMPARISSON";
			break;
		case MOD:
			// TODO Mod
			instr = "MOD";
			break;
		}
		
		visArg.add(new AsmOPER(instr + " `d0, `s0, `s1", uses, defs, null));
		
		return temp;
	}
	
	@Override
	public Temp visit(ImcUNOP unOp, Vector<AsmInstr> visArg) {
		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();
		
		Temp temp = new Temp();
		defs.add(temp);
		
		uses.add(unOp.subExpr.accept(this, visArg));
		
		switch (unOp.oper) {
		case NEG:
			visArg.add(new AsmOPER("NEG `d0, `s0", uses, defs, null));
			break;
		case NOT:
			visArg.add(new AsmOPER("SUB `d0, `s0, 1", uses, defs, null));
			visArg.add(new AsmOPER("NEG `d0, `s0", defs, defs, null));
			return temp;
		}
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

	@Override
	public Temp visit(ImcCALL call, Vector<AsmInstr> visArg) {
		Vector<Label> jumps = new Vector<>();
		jumps.add(call.label);
		
		int offset = call.args().size() * 8;
		for(ImcExpr expr: call.args()) {
			offset -= 8;
			
			Vector<Temp> uses = new Vector<>();
			uses.add(expr.accept(this, visArg));
			visArg.add(new AsmOPER("STO `s0,$254,"+offset , uses, null, null));
		}
		
		visArg.add(new AsmOPER("PUSHJ $15," + call.label.name, null, null, jumps));
		return new Temp();
	}
	
}
