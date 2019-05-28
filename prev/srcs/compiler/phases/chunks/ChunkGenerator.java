/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.Vector;

import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.layout.*;
import compiler.data.imcode.*;
import compiler.data.chunk.*;
import compiler.phases.frames.*;
import compiler.phases.imcgen.*;

/**
 * @author sliva
 *
 */
public class ChunkGenerator extends AbsFullVisitor<Object, Object> {
	
	@Override
	public Object visit(AbsFunDef funDef, Object visArg) {		
		Frame funFrame = Frames.frames.get(funDef);
		
		ImcExpr bodyExpr = ImcGen.exprImCode.get(funDef.value);

		Label entryLabel = new Label();
		Label exitLabel = new Label();
		
		Vector<ImcStmt> funStmts = new Vector<>();
		funStmts.add(new ImcLABEL(entryLabel));
		funStmts.addAll(bodyExpr.accept(new StmtCanonizer(), funFrame.RV));
		funStmts.add(new ImcJUMP(exitLabel));
				
		Chunks.codeChunks.add(new CodeChunk(funFrame, funStmts, entryLabel, exitLabel));
		return super.visit(funDef, visArg);
	}
	
	@Override
	public Object visit(AbsAtomExpr atomExpr, Object visArg) {
		switch(atomExpr.type) {
		case STR:
			Chunks.dataChunks.add(new DataChunk(Frames.strings.get(atomExpr)));
			break;
		default:
		break;
		}
		return super.visit(atomExpr, visArg);
	}
	
	@Override
	public Object visit(AbsVarDecl varDecl, Object visArg) {
		if(Frames.accesses.get(varDecl) instanceof AbsAccess) {
			Chunks.dataChunks.add(new DataChunk((AbsAccess)Frames.accesses.get(varDecl)));
		}
		return super.visit(varDecl, visArg);
	}
}
