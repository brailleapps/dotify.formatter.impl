package org.daisy.dotify.formatter.impl.page;

import org.daisy.dotify.api.formatter.FormattingTypes.BreakBefore;
import org.daisy.dotify.formatter.impl.core.Block;
import org.daisy.dotify.formatter.impl.core.BlockContext;
import org.daisy.dotify.formatter.impl.core.LayoutMaster;
import org.daisy.dotify.formatter.impl.row.AbstractBlockContentManager;
import org.daisy.dotify.formatter.impl.row.BlockStatistics;
import org.daisy.dotify.formatter.impl.row.LineProperties;
import org.daisy.dotify.formatter.impl.row.RowImpl;
import org.daisy.dotify.formatter.impl.search.DefaultContext;

/**
 * Provides data about a single rendering scenario.
 * 
 * @author Joel Håkansson
 */
abstract class BlockProcessor {
	private RowGroupProvider rowGroupProvider;
	
	/** Returns true if a new RowGroupSequence was created, false otherwise. */
	protected abstract boolean maybeNewRowGroupSequence(BreakBefore breakBefore, VerticalSpacing vs);
	protected abstract boolean hasSequence();
	protected abstract void addRowGroup(RowGroup rg);
	
	BlockProcessor() { }

	BlockProcessor(BlockProcessor template) {
		this.rowGroupProvider = copyUnlessNull(template.rowGroupProvider);
	}
	
	protected void loadBlock(LayoutMaster master, Block g, BlockContext bc) {
		AbstractBlockContentManager bcm = g.getBlockContentManager(bc);
		boolean newSequence = false;
		if (!hasSequence()
		    || g.getBreakBeforeType() != BreakBefore.AUTO
		    || g.getVerticalPosition() != null) {
			newSequence = maybeNewRowGroupSequence(
				g.getBreakBeforeType(),
				g.getVerticalPosition() != null
					? new VerticalSpacing(g.getVerticalPosition(),
					                      new RowImpl("", bcm.getLeftMarginParent(), bcm.getRightMarginParent()))
					: null);
		}
		int keepWithNext = 0;
		if (newSequence) {
			keepWithNext = -1;
		} else if (rowGroupProvider!=null) {
			keepWithNext = rowGroupProvider.getKeepWithNext();
		}
		rowGroupProvider = new RowGroupProvider(master, g, bcm, bc, keepWithNext);
	}
	
	protected void processNextRowGroup(DefaultContext context, LineProperties lineProps) {
		if (hasNextInBlock()) {
			addRowGroup(rowGroupProvider.next(context, lineProps));
		}
	}
	
	protected boolean hasNextInBlock() {
		if (rowGroupProvider != null) {
			if (rowGroupProvider.hasNext()) {
				return true;
			} else {
				rowGroupProvider.close();
			}
		}
		return false;
	}
	
	private RowGroupProvider copyUnlessNull(RowGroupProvider template) {
		return template==null?null:new RowGroupProvider(template);
	}

	/**
	 * Gets the current block's statistics, or null if no block has been loaded.
	 * @return returns the block statistics, or null
	 */
	BlockStatistics getBlockStatistics() {
		if (rowGroupProvider!=null) {
			return rowGroupProvider.getBlockStatistics();
		} else {
			return null;
		}
	}

}