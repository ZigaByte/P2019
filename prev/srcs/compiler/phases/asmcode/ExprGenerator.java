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

	Temp temp = null;
	
	public ExprGenerator() {
		this(new Temp());
	}
	
	public ExprGenerator(Temp t) {
		this.temp = t;
	}
	
	
	@Override
	public Temp visit(ImcCONST constant, Vector<AsmInstr> visArg) {
		Vector<Temp> defs = new Vector<>();
		
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
		
		visArg.add(new AsmOPER("SETL `d0," + l, null, defs, null));
		if(ml != 0)
			visArg.add(new AsmOPER("INCML `s0," + ml, defs, defs, null));
		if(mh != 0)
			visArg.add(new AsmOPER("INCMH `s0," + mh, defs, defs, null));
		if(h != 0)
			visArg.add(new AsmOPER("INCH `s0," + h, defs, defs, null));

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
			visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defs, null));
			visArg.add(new AsmOPER("ADD `d0,`s0,1", defs, defs, null));
			visArg.add(new AsmOPER("DIV `d0,`s0,2", defs, defs, null));
			visArg.add(new AsmOPER("GET `d0,rR", null, defs, null));
			return temp;
		case LTH:{
			Temp inst = uses.get(0);
			uses.removeElementAt(0);
			uses.insertElementAt(inst, 1);}
		case GTH:
			visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defs, null));
			visArg.add(new AsmOPER("ADD `d0,`s0,1", defs, defs, null));
			visArg.add(new AsmOPER("SR `d0,`s0,1", defs, defs, null));
			return temp;
		case LEQ:{
			Temp inst = uses.get(0);
			uses.removeElementAt(0);
			uses.insertElementAt(inst, 1);}
		case GEQ:
			visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defs, null));
			visArg.add(new AsmOPER("ADD `d0,`s0,2", defs, defs, null));
			visArg.add(new AsmOPER("AND `d0,`s0,2", defs, defs, null));
			visArg.add(new AsmOPER("SR `d0,`s0,1", defs, defs, null));
			return temp;
		case NEQ:
			visArg.add(new AsmOPER("CMP `d0,`s0,`s1", uses, defs, null));
			visArg.add(new AsmOPER("ADD `d0,`s0,2", defs, defs, null));
			visArg.add(new AsmOPER("AND `d0,`s0,1", defs, defs, null));
			return temp;
		case MOD:
			visArg.add(new AsmOPER("DIV `d0,`s0,`s1", uses, defs, null));
			visArg.add(new AsmOPER("GET `d0,rR", null, defs, null));
			return temp;
		}
		
		visArg.add(new AsmOPER(instr + " `d0,`s0,`s1", uses, defs, null));
		
		return temp;
	}
	
	@Override
	public Temp visit(ImcUNOP unOp, Vector<AsmInstr> visArg) {
		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();
		
		defs.add(temp);
		
		uses.add(unOp.subExpr.accept(this, visArg));
		
		switch (unOp.oper) {
		case NEG:
			visArg.add(new AsmOPER("NEG `d0,`s0", uses, defs, null));
			break;
		case NOT:
			visArg.add(new AsmOPER("SUB `d0,`s0,1", uses, defs, null));
			visArg.add(new AsmOPER("NEG `d0,`s0", defs, defs, null));
			return temp;
		}
		return temp;
	}
	
	@Override
	public Temp visit(ImcNAME name, Vector<AsmInstr> visArg) {
		Vector<Temp> uses = new Vector<>();
		uses.add(new Temp());
		
		Vector<Temp> defs = new Vector<>();

		defs.add(temp);
		
		//visArg.add(new AsmOPER("GET `d0," + name.label.name, null, uses, null));
		
		visArg.add(new AsmOPER("LDA `d0,"+ name.label.name, null, defs, null));
		
		return temp;
	}
	
	@Override
	public Temp visit(ImcMEM mem, Vector<AsmInstr> visArg) {

		Vector<Temp> defs = new Vector<>();
		Vector<Temp> uses = new Vector<>();

		defs.add(temp);		
		uses.add(mem.addr.accept(this, visArg));

		visArg.add(new AsmOPER("LDO `d0,`s0,0", uses, defs, null));
		
		return temp;
	}

	@Override
	public Temp visit(ImcCALL call, Vector<AsmInstr> visArg) {
		Vector<Label> jumps = new Vector<>();
		jumps.add(call.label);
		
		int offset = call.args().size() * 8;
		
		// Store parameters
		for(ImcExpr expr: call.args()) {
			offset -= 8;
			
			Temp temp = expr.accept(this, visArg);
			
			Vector<Temp> uses = new Vector<>();
			uses.add(temp);
			visArg.add(new AsmOPER("STO `s0,$254,"+offset , uses, null, null));
		}
		
		visArg.add(new AsmOPER("PUSHJ $15," + call.label.name, null, null, jumps));
		
		// Load return value
		Temp result = temp;
		Vector<Temp> defs = new Vector<Temp>();
		defs.add(result);
		visArg.add(new AsmOPER("LDO `d0,$254,0" , null, defs, null));		
		
		return result;
	}
	
}
