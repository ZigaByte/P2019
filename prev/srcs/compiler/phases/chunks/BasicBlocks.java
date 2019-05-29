package compiler.phases.chunks;

import java.util.Vector;

import compiler.data.chunk.CodeChunk;
import compiler.data.imcode.ImcCJUMP;
import compiler.data.imcode.ImcJUMP;
import compiler.data.imcode.ImcLABEL;
import compiler.data.imcode.ImcStmt;
import compiler.data.layout.Label;

class Block{
	Vector<ImcStmt> stmts = new Vector<>();
	Label start;
	Label jump; // Either unconditional jump or posLabel we will jump to
	
	Block nextBlock; // Only for CJUMP
	
	boolean isRoot = true;
	boolean isTaken = false;
	
	public boolean isCJUMP() {
		return stmts.size() >= 0 && stmts.lastElement() instanceof ImcCJUMP; 
	}
	
	@Override
	public String toString() {
		System.out.println(start.name + ":");
		for(ImcStmt stmt:stmts) {
			System.out.println(stmt + (stmt instanceof ImcCJUMP ? " " +((ImcCJUMP)stmt).posLabel.name:"") );
		}
		System.out.println("JMP " + jump.name);
		System.out.println("Next block " + nextBlock);
		return super.toString();
	}
}

public class BasicBlocks {
	
	private CodeChunk codeChunk;
	
	private Vector<Block> blocks = new Vector<>();
	
	public BasicBlocks(CodeChunk codeChunk) {
		this.codeChunk = codeChunk;
		generateBlocks();
		mergeCJumps();
		mergeJumps();	
	}
	
	public CodeChunk getChunk() {
		Vector<ImcStmt> stmts = new Vector<>();
		for(Block b: blocks) {
			stmts.addAll(b.stmts);
		}		
		return new CodeChunk(codeChunk.frame, stmts, codeChunk.entryLabel, codeChunk.exitLabel);
	}
	
	private boolean cycle(Block next, Block current) {
		if(next == null) return false;
		return next == current || cycle(next.nextBlock, current);
	}
	
	private void mergeCJumps() {
		// Find the block of the negative label
		for(Block currentBlock : blocks) {
			if(currentBlock.isCJUMP()) {
				// Find and connect the right block
				for(Block nextBlock : blocks) {
					if(nextBlock.start == ((ImcCJUMP)currentBlock.stmts.lastElement()).negLabel
							&& !cycle(nextBlock, currentBlock) && !nextBlock.isTaken) {
						currentBlock.nextBlock = nextBlock;
						nextBlock.isRoot = false;
						nextBlock.isTaken = true;
					}
				}
			}
		}
		
		Vector<Block> roots = new Vector<>();
		while(blocks.size() > 0) {
			Block currentBlock = blocks.get(0);
			blocks.remove(0);
			while (currentBlock.isRoot && currentBlock.nextBlock != null) {
				Block nextBlock = currentBlock.nextBlock;
				currentBlock.stmts.addAll(nextBlock.stmts);
				
				currentBlock.jump = nextBlock.jump;
				currentBlock.nextBlock = nextBlock.nextBlock;
			}
			if(currentBlock.isRoot) {
				roots.add(currentBlock);
			}
		}
		blocks = roots;
	}
	
	private void mergeJumps() {
		for(Block currentBlock : blocks) {
			if(!currentBlock.isCJUMP()) {
				// Find and connect the right block
				for(Block nextBlock : blocks) {
					if(nextBlock.start == currentBlock.jump && !cycle(nextBlock, currentBlock) && !nextBlock.isTaken) {
						currentBlock.nextBlock = nextBlock;
						nextBlock.isRoot = false;
						nextBlock.isTaken = true;
					}
				}
			}
		}
		
		
		Vector<Block> merged = new Vector<>();
		while(blocks.size() > 0) {
			Block currentBlock = blocks.get(0);
			blocks.remove(0);

			while (currentBlock.isRoot && currentBlock.nextBlock != null) {
				Block nextBlock = currentBlock.nextBlock;
				
				currentBlock.stmts.remove(currentBlock.stmts.lastElement()); // Remove the jump
				currentBlock.stmts.addAll(nextBlock.stmts);
				
				currentBlock.jump = nextBlock.jump;
				currentBlock.nextBlock = nextBlock.nextBlock;
			}
			if(currentBlock.isRoot) {
				merged.add(currentBlock);
			}
		}
		blocks = merged;
	}

	private void replaceLabel(Vector<ImcStmt> stmts, Label toReplace, Label replacement) {
		for(ImcStmt stmt : stmts) {
			if(stmt instanceof ImcJUMP) {
				if (((ImcJUMP)stmt).label == toReplace)
					((ImcJUMP)stmt).label = replacement;
			}else if(stmt instanceof ImcCJUMP) {
				if (((ImcCJUMP)stmt).negLabel == toReplace)
					((ImcCJUMP)stmt).negLabel = replacement;
				if (((ImcCJUMP)stmt).posLabel == toReplace)
					((ImcCJUMP)stmt).posLabel = replacement;
			}
		}
	}
	
	public void generateBlocks() {
		Vector<ImcStmt> stmts = codeChunk.stmts();
		
		Block currentBlock = new Block();
		for(ImcStmt currentStmt : stmts) {
			//System.out.println(currentStmt + " " + (currentStmt instanceof ImcLABEL ? ((ImcLABEL)currentStmt).label.name+"":""));
			
			if(currentBlock.start == null && currentStmt instanceof ImcLABEL) {
				// Add the start label if needed
				currentBlock.start = ((ImcLABEL)currentStmt).label;
				currentBlock.stmts.add(currentStmt);
				
			} else if(currentStmt instanceof ImcLABEL) {
				if(currentBlock.stmts.size() == 1) {
					// Remove this label if there are no stmts in the block yet
					replaceLabel(stmts, ((ImcLABEL)currentStmt).label, currentBlock.start);
				} else {
					// Add a jump at the end and finish the block
					currentBlock.jump = ((ImcLABEL)currentStmt).label;
					currentBlock.stmts.add(new ImcJUMP(currentBlock.jump));
					blocks.add(currentBlock);	
					
					currentBlock = new Block();
					currentBlock.start = ((ImcLABEL)currentStmt).label;
					currentBlock.stmts.add(currentStmt);
				}
			} else if(currentBlock.start != null 
					&& (currentStmt instanceof ImcJUMP || currentStmt instanceof ImcCJUMP)){
				
				if(currentBlock.stmts.size() == 1 && currentStmt instanceof ImcJUMP) {
					// Replace this label with what we are jumping to and restart block
					replaceLabel(stmts, currentBlock.start, ((ImcJUMP)currentStmt).label);
					currentBlock = new Block();
				} else {
					
					if(currentStmt instanceof ImcJUMP) {
						currentBlock.jump = ((ImcJUMP)currentStmt).label;
					} else {
						currentBlock.jump = ((ImcCJUMP)currentStmt).posLabel;	
					}
					currentBlock.stmts.add(currentStmt);
					
					blocks.add(currentBlock);				
					currentBlock = new Block();
				}
			} else if(currentBlock.start != null ) {
				currentBlock.stmts.add(currentStmt);
			} else{
				// Instruction not needed apparently
			}
		}
	}
}
