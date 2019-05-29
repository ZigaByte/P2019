/**
 * @author sliva
 */
package compiler.phases.chunks;

import java.util.*;
import compiler.data.asmcode.Code;
import compiler.data.chunk.*;
import compiler.data.imcode.ImcCONST;
import compiler.data.imcode.ImcESTMT;
import compiler.data.imcode.ImcLABEL;
import compiler.data.imcode.ImcMOVE;
import compiler.data.imcode.ImcStmt;
import compiler.data.imcode.ImcTEMP;
import compiler.data.layout.Temp;
import compiler.phases.*;

/**
 * @author sliva
 */
public class Chunks extends Phase {

	public static Vector<DataChunk> dataChunks = new Vector<DataChunk>();

	public static Vector<CodeChunk> codeChunks = new Vector<CodeChunk>();

	public Chunks() {
		super("chunks");
	}

	public void log() {
		ChunkLogger chunkLogger = new ChunkLogger(logger);
		for (DataChunk dataChunk : dataChunks)
			chunkLogger.log(dataChunk);
		for (CodeChunk codeChunk : codeChunks)
			chunkLogger.log(codeChunk);
	}

	public static void cleanChunks() {
		Vector<CodeChunk> chunks = new Vector<>();
		for(CodeChunk codeChunk: codeChunks) {
			BasicBlocks bb = new BasicBlocks(codeChunk);
			chunks.add(bb.getChunk());
		}		
		codeChunks = chunks;
	}
	

}
