/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
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

	private ExprCanonizer exprCanonizer;
	private StmtCanonizer stmtCanonizer;
	
	private ExprCanonizer exprCanonizer() {
		if (exprCanonizer == null)
			exprCanonizer = new ExprCanonizer();
		return exprCanonizer;
	}
	private StmtCanonizer stmtCanonizer() {
		if (stmtCanonizer == null)
			stmtCanonizer = new StmtCanonizer();
		return stmtCanonizer;
	}
	
	@Override
	public Object visit(AbsFunDef funDef, Object visArg) {
		Frame funFrame = Frames.frames.get(funDef);
		
		ImcExpr bodyExpr = ImcGen.exprImCode.get(funDef.value);
		ImcStmt funStmt = new ImcMOVE(new ImcTEMP(funFrame.RV), bodyExpr);
				
		Chunks.codeChunks.add(new CodeChunk(funFrame, funStmt.accept(stmtCanonizer(), null), new Label(), new Label()));
		return super.visit(funDef, visArg);
	}
	
	
}
