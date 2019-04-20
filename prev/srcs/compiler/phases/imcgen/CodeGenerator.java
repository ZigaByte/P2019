/**
 * @author sliva
 */
package compiler.phases.imcgen;

import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import compiler.data.abstree.AbsArrExpr;
import compiler.data.abstree.AbsAssignStmt;
import compiler.data.abstree.AbsAtomExpr;
import compiler.data.abstree.AbsAtomType.Type;
import compiler.data.abstree.AbsBinExpr;
import compiler.data.abstree.AbsBlockExpr;
import compiler.data.abstree.AbsCastExpr;
import compiler.data.abstree.AbsDelExpr;
import compiler.data.abstree.AbsExpr;
import compiler.data.abstree.AbsExprStmt;
import compiler.data.abstree.AbsFunDecl;
import compiler.data.abstree.AbsFunDef;
import compiler.data.abstree.AbsFunName;
import compiler.data.abstree.AbsIfStmt;
import compiler.data.abstree.AbsNewExpr;
import compiler.data.abstree.AbsRecExpr;
import compiler.data.abstree.AbsStmt;
import compiler.data.abstree.AbsStmts;
import compiler.data.abstree.AbsUnExpr;
import compiler.data.abstree.AbsVarDecl;
import compiler.data.abstree.AbsVarName;
import compiler.data.abstree.AbsWhileStmt;
import compiler.data.abstree.visitor.AbsFullVisitor;
import compiler.data.imcode.ImcBINOP;
import compiler.data.imcode.ImcCALL;
import compiler.data.imcode.ImcCJUMP;
import compiler.data.imcode.ImcCONST;
import compiler.data.imcode.ImcESTMT;
import compiler.data.imcode.ImcExpr;
import compiler.data.imcode.ImcJUMP;
import compiler.data.imcode.ImcLABEL;
import compiler.data.imcode.ImcMEM;
import compiler.data.imcode.ImcMOVE;
import compiler.data.imcode.ImcNAME;
import compiler.data.imcode.ImcSTMTS;
import compiler.data.imcode.ImcStmt;
import compiler.data.imcode.ImcUNOP;
import compiler.data.imcode.ImcBINOP.Oper;
import compiler.data.imcode.visitor.ImcVisitor;
import compiler.data.layout.Frame;
import compiler.data.layout.Label;
import compiler.data.type.SemCharType;
import compiler.data.type.SemType;
import compiler.phases.frames.Frames;
import compiler.phases.seman.SemAn;

/**
 * Intermediate code generator.
 * 
 * This is a plain full visitor
 * 
 * @author sliva
 */
public class CodeGenerator extends AbsFullVisitor<Object, Stack<Frame>> {

	private AddrGenerator addrGenerator;
	
	private AddrGenerator getAddrGenerator() {
		if (addrGenerator == null)
			addrGenerator = new AddrGenerator(this);
		return addrGenerator;
	}

	
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
			ImcGen.exprImCode.put(atomExpr, new ImcNAME(Frames.strings.get(atomExpr).label));
			break;
		}
		return ImcGen.exprImCode.get(atomExpr);
	}
	
	@Override
	public Object visit(AbsUnExpr unExpr, Stack<Frame> visArg) {
		ImcExpr subExpr = (ImcExpr) unExpr.subExpr.accept(this, visArg);
		
		switch (unExpr.oper) {
		case SUB:
			ImcGen.exprImCode.put(unExpr, new ImcUNOP(ImcUNOP.Oper.NEG, subExpr));
			break;
		case NOT:
			ImcGen.exprImCode.put(unExpr, new ImcUNOP(ImcUNOP.Oper.NOT, subExpr));
			break;
			
		case DATA: // @
			ImcGen.exprImCode.put(unExpr, new ImcMEM(subExpr));
			break;
			
		case ADDR: // $
			ImcGen.exprImCode.put(unExpr, (ImcExpr) unExpr.subExpr.accept(getAddrGenerator(), visArg));
			return subExpr;
			
		default:
			// Nothing to do really with other prefixes
			break;
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
	
	@Override
	public Object visit(AbsDelExpr delExpr, Stack<Frame> visArg) {
		Vector<ImcExpr> args = new Vector<>();
		args.add((ImcExpr)delExpr.expr.accept(this, visArg));
		ImcGen.exprImCode.put(delExpr, new ImcCALL(new Label("del"), args));
		return ImcGen.exprImCode.get(delExpr);
	}
	
	@Override
	public Object visit(AbsNewExpr newExpr, Stack<Frame> visArg) {
		// TODO Auto-generated method stub
		long size = SemAn.isType.get(newExpr.type).actualType().size();
		
		Vector<ImcExpr> args = new Vector<>();
		args.add(new ImcCONST(size));
		
		ImcGen.exprImCode.put(newExpr, new ImcCALL(new Label("new"), args));
		return ImcGen.exprImCode.get(newExpr);
	}
	
	@Override
	public Object visit(AbsVarName varName, Stack<Frame> visArg) {
		ImcGen.exprImCode.put(varName, new ImcMEM(varName.accept(getAddrGenerator(), visArg)));
		return ImcGen.exprImCode.get(varName);
	}
	
	
	@Override
	public Object visit(AbsFunDef funDef, Stack<Frame> visArg) {
		visArg.push(Frames.frames.get(funDef));
		super.visit(funDef, visArg);
		visArg.pop();
		return null;
	}
	
	@Override
	public Object visit(AbsArrExpr arrExpr, Stack<Frame> visArg) {
		ImcGen.exprImCode.put(arrExpr, new ImcMEM(arrExpr.accept(getAddrGenerator(), visArg)));
		return ImcGen.exprImCode.get(arrExpr);
	}
	
	@Override
	public Object visit(AbsRecExpr recExpr, Stack<Frame> visArg) {
		ImcGen.exprImCode.put(recExpr, new ImcMEM(recExpr.accept(getAddrGenerator(), visArg)));
		return ImcGen.exprImCode.get(recExpr);
	}
	
	
	@Override
	public Object visit(AbsFunName funName, Stack<Frame> visArg) {
		Vector<ImcExpr> imcArgs = new Vector<>();
		for(AbsExpr arg: funName.args.args()) {
			imcArgs.add((ImcExpr) arg.accept(this, visArg));
		}
		Label l = Frames.frames.get((AbsFunDecl)SemAn.declaredAt.get(funName)).label;
		
		ImcGen.exprImCode.put(funName, new ImcCALL(l, imcArgs));
		return ImcGen.exprImCode.get(funName);
	}
	
	@Override
	public ImcExpr visit(AbsBlockExpr blockExpr, Stack<Frame> visArg) {
		blockExpr.stmts.accept(this, visArg);		
		
		ImcExpr expr = (ImcExpr) blockExpr.expr.accept(this, visArg);
		
		ImcGen.exprImCode.put(blockExpr, expr);
		return ImcGen.exprImCode.get(blockExpr);
	}
	
	@Override
	public Object visit(AbsCastExpr castExpr, Stack<Frame> visArg) {
		ImcExpr expr = (ImcExpr) castExpr.expr.accept(this, visArg);
		if(SemAn.isType.get(castExpr.type).actualType() instanceof SemCharType) {
			expr = new ImcBINOP(Oper.MOD, expr, new ImcCONST(256));
		}
		ImcGen.exprImCode.put(castExpr, expr);
		return ImcGen.exprImCode.get(castExpr);
	}
	
	@Override
	public Object visit(AbsExprStmt exprStmt, Stack<Frame> visArg) {
		ImcGen.stmtImCode.put(exprStmt, new ImcESTMT((ImcExpr)exprStmt.expr.accept(this, visArg)));
		return ImcGen.stmtImCode.get(exprStmt);
	}
	
	@Override
	public Object visit(AbsAssignStmt assignStmt, Stack<Frame> visArg) {
		ImcExpr dst = (ImcExpr) assignStmt.dst.accept(this, visArg);
		ImcExpr src = (ImcExpr) assignStmt.src.accept(this, visArg);
		
		ImcGen.stmtImCode.put(assignStmt, new ImcMOVE(dst, src));
		return ImcGen.stmtImCode.get(assignStmt);
	}
	
	@Override
	public Object visit(AbsIfStmt ifStmt, Stack<Frame> visArg) {
		ImcExpr cond = (ImcExpr) ifStmt.cond.accept(this, visArg);
		
		ImcSTMTS thenStmts = (ImcSTMTS)ifStmt.thenStmts.accept(this, visArg);
		ImcSTMTS elseStmts = (ImcSTMTS)ifStmt.elseStmts.accept(this, visArg);
		
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		
		Label thenLabel = new Label();
		Label elseLabel = new Label();
		Label endLabel = new Label();
		
		stmts.add(new ImcCJUMP(cond, thenLabel, elseLabel));
		stmts.add(new ImcLABEL(thenLabel));
		stmts.add(thenStmts);
		stmts.add(new ImcJUMP(endLabel));
		stmts.add(new ImcLABEL(elseLabel));
		stmts.add(elseStmts);
		stmts.add(new ImcJUMP(endLabel));
		stmts.add(new ImcLABEL(endLabel));
		
		ImcGen.stmtImCode.put(ifStmt, new ImcSTMTS(stmts));
		return ImcGen.stmtImCode.get(ifStmt);	
	}
	
	@Override
	public Object visit(AbsWhileStmt whileStmt, Stack<Frame> visArg) {
		ImcExpr cond = (ImcExpr) whileStmt.cond.accept(this, visArg);
		
		ImcSTMTS doStmts = (ImcSTMTS) whileStmt.stmts.accept(this, visArg);
		
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		
		Label startLabel = new Label();
		Label doLabel = new Label();
		Label endLabel = new Label();

		stmts.add(new ImcLABEL(startLabel));
		stmts.add(new ImcCJUMP(cond, doLabel, endLabel));
		stmts.add(new ImcLABEL(doLabel));
		stmts.add(doStmts);
		stmts.add(new ImcJUMP(startLabel));
		stmts.add(new ImcLABEL(endLabel));
		
		ImcGen.stmtImCode.put(whileStmt, new ImcSTMTS(stmts));
		return ImcGen.stmtImCode.get(whileStmt);
	}
	
	@Override
	public Object visit(AbsStmts stmts, Stack<Frame> visArg) {
		Vector<ImcStmt> sequence = new Vector<ImcStmt>();
		for(AbsStmt stmt: stmts.stmts()) {
			sequence.add((ImcStmt)stmt.accept(this, visArg));
		}
		return new ImcSTMTS(sequence);	
	}
	
}
