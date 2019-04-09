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
		public long argsSize = 0;
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
		
		Frame frame = new Frame(new Label(funDef.name), funContext.depth, funContext.locsSize, funContext.argsSize);
		Frames.frames.put(funDef, frame);
		return null;
	}
	
	@Override
	public Object visit(AbsVarDecl varDecl, Context visArg) {
		SemType type = SemAn.isType.get(varDecl.type);
		((FunContext)visArg).locsSize += type.size();
		
		return null;
	}
	
	@Override
	public Object visit(AbsParDecl parDecl, Context visArg) {
		SemType type = SemAn.isType.get(parDecl.type);
		((FunContext)visArg).locsSize += type.size();
		
		return super.visit(parDecl, visArg);
	}
	
}
