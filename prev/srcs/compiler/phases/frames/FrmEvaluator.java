/**
 * @author sliva
 */
package compiler.phases.frames;

import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.*;
import compiler.data.layout.*;
import compiler.phases.seman.*;

/**
 * Computing function frames and accesses.
 * 
 * @author sliva
 */
public class FrmEvaluator extends AbsFullVisitor<Object, FrmEvaluator.Context> {

	/**
	 * The context {@link FrmEvaluator} uses while computing function frames and
	 * variable accesses.
	 * 
	 * @author sliva
	 */
	protected abstract class Context {
	}

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 * 
	 * @author sliva
	 */
	private class FunContext extends Context {
		public int depth = 0;
		public long locsSize = 0;
		public long argsSize = 0; // Start with Static link size
		public long parsSize = new SemPtrType(new SemVoidType()).size();
	}

	/**
	 * Record context, i.e., used when traversing record definition and computing
	 * record component acceses.
	 * 
	 * @author sliva
	 */
	private class RecContext extends Context {
		public long compsSize = 0;
	}
	
	@Override
	public Object visit(AbsSource source, Context visArg) {
		FunContext fc = new FunContext();
		fc.depth = 0;
		return super.visit(source, fc);
	}

	@Override
	public Object visit(AbsFunDef funDef, Context visArg) {
		FunContext funContext = new FunContext();
		funContext.depth = ((FunContext)visArg).depth + 1;
		
		super.visit(funDef, funContext);
		
		Label label = funContext.depth == 1 ? new Label(funDef.name) : new Label();
		
		funContext.argsSize += new SemPtrType(new SemVoidType()).size(); // Add the Static link
		
		Frame frame = new Frame(label, funContext.depth, funContext.locsSize, funContext.argsSize);
		Frames.frames.put(funDef, frame);
		return null;
	}
	
	@Override
	public Object visit(AbsVarDecl varDecl, Context visArg) {
		FunContext funContext = (FunContext)visArg;
		
		SemType type = SemAn.isType.get(varDecl.type);
		funContext.locsSize += type.size();

		Access access = null;
		if(funContext.depth == 0) {
			access = new AbsAccess(type.size(), new Label(varDecl.name));		
		}else {
			access = new RelAccess(type.size(), -funContext.locsSize, funContext.depth);
		}
		Frames.accesses.put(varDecl, access);
	
		
		return super.visit(varDecl, new RecContext());
	}
	
	@Override
	public Object visit(AbsTypDecl typDecl, Context visArg) {
		return super.visit(typDecl, new RecContext());
	}
	
	@Override
	public Object visit(AbsCompDecl compDecl, Context visArg) {
		RecContext recContext = (RecContext) visArg;
		
		SemType type = SemAn.isType.get(compDecl.type);
		recContext.compsSize += type.size();
		
		Frames.accesses.put(compDecl, new RelAccess(type.size(), recContext.compsSize - type.size(), 0));
		
		return super.visit(compDecl, visArg);
	}
	
	@Override
	public Object visit(AbsParDecl parDecl, Context visArg) {
		FunContext funContext = (FunContext)visArg;
		
		SemType type = SemAn.isType.get(parDecl.type);
		funContext.parsSize += type.size();
		
		Frames.accesses.put(parDecl, new RelAccess(type.size(), funContext.parsSize - type.size(), funContext.depth));
		
		return super.visit(parDecl, visArg);
	}
	
	@Override
	public Object visit(AbsFunName funName, Context visArg) {
		FunContext funContext = (FunContext)visArg;
		
		int argsSize = (Integer)funName.args.accept(this, visArg);
		funContext.argsSize = Math.max(argsSize, funContext.argsSize);
		
		return super.visit(funName, visArg);
	}
	
	@Override
	public Object visit(AbsArgs args, Context visArg) {
		int size = 0;
		for(AbsExpr expr: args.args()) {
			SemType type = SemAn.ofType.get(expr);
			size += type.size();
		}
		return size;
	}
	
	@Override
	public Object visit(AbsAtomExpr atomExpr, Context visArg) {
		switch (atomExpr.type) {
		case STR: {
			AbsAccess stringAccess = new AbsAccess(8 * (atomExpr.expr.length()-1), new Label(), atomExpr.expr);
			Frames.strings.put(atomExpr, stringAccess);
			break;
		}
		default:
			break;
		}
		return super.visit(atomExpr, visArg);
	}
	
}
