package compiler.phases.imcgen;

import java.util.Stack;

import compiler.data.abstree.AbsArrExpr;
import compiler.data.abstree.AbsAtomExpr;
import compiler.data.abstree.AbsRecExpr;
import compiler.data.abstree.AbsVarDecl;
import compiler.data.abstree.AbsVarName;
import compiler.data.abstree.AbsAtomExpr.Type;
import compiler.data.abstree.visitor.AbsFullVisitor;
import compiler.data.imcode.ImcBINOP;
import compiler.data.imcode.ImcCONST;
import compiler.data.imcode.ImcExpr;
import compiler.data.imcode.ImcMEM;
import compiler.data.imcode.ImcNAME;
import compiler.data.imcode.ImcTEMP;
import compiler.data.imcode.ImcBINOP.Oper;
import compiler.data.layout.AbsAccess;
import compiler.data.layout.Access;
import compiler.data.layout.Frame;
import compiler.data.layout.RelAccess;
import compiler.phases.frames.Frames;
import compiler.phases.seman.SemAn;

public class AddrGenerator extends AbsFullVisitor<ImcExpr, Stack<Frame>>{

	private CodeGenerator codeGenerator;
	
	public AddrGenerator(CodeGenerator codeGenerator) {
		this.codeGenerator = codeGenerator;
	}
	
	@Override
	public ImcExpr visit(AbsAtomExpr atomExpr, Stack<Frame> visArg) {
		if(atomExpr.type == Type.STR) {
			return new ImcNAME(((AbsAccess)Frames.strings.get(atomExpr)).label);
		}
		return super.visit(atomExpr, visArg);
	}
	
	@Override
	public ImcExpr visit(AbsVarName varName, Stack<Frame> visArg) {
		Access access = Frames.accesses.get((AbsVarDecl)SemAn.declaredAt.get(varName));
		
		if(access instanceof AbsAccess) {
			return new ImcNAME(((AbsAccess) access).label);
		} else if( access instanceof RelAccess){
			RelAccess relAccess = (RelAccess) access;
			Frame currentFrame = visArg.peek();
			
			ImcExpr expr1 = new ImcTEMP(currentFrame.FP);
			for(int i = 0; i < currentFrame.depth - relAccess.depth; i++) {
				expr1 = new ImcMEM(expr1);
			}
			return new ImcBINOP(Oper.ADD, expr1, new ImcCONST(relAccess.offset));
		}
		
		return super.visit(varName, visArg);
	}
	
	@Override
	public ImcExpr visit(AbsArrExpr arrExpr, Stack<Frame> visArg) {
		ImcExpr expr1 = (ImcExpr) arrExpr.array.accept(codeGenerator, visArg);
		ImcExpr expr2 = (ImcExpr) arrExpr.index.accept(codeGenerator, visArg);

		long size = SemAn.ofType.get(arrExpr).size();
		return new ImcBINOP(Oper.ADD, expr1, new ImcBINOP(Oper.MUL, expr2, new ImcCONST(size)));
	}
	
	@Override
	public ImcExpr visit(AbsRecExpr recExpr, Stack<Frame> visArg) {
		ImcExpr expr1 = recExpr.record.accept(this, visArg);
		ImcExpr expr2 = recExpr.comp.accept(this, visArg); // but we only want offset
		
		if(expr2 instanceof ImcBINOP) {
			return new ImcBINOP(Oper.ADD, expr1, ((ImcBINOP)expr2).sndExpr);
		}
		
		return new ImcBINOP(Oper.ADD, expr1, expr2);
	}
}
