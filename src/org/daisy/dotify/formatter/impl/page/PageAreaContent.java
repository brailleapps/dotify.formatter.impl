package org.daisy.dotify.formatter.impl.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.daisy.dotify.formatter.impl.core.Block;
import org.daisy.dotify.formatter.impl.core.BlockContext;
import org.daisy.dotify.formatter.impl.core.PageAreaBuilderImpl;
import org.daisy.dotify.formatter.impl.row.AbstractBlockContentManager;
import org.daisy.dotify.formatter.impl.row.RowImpl;

class PageAreaContent {
	private final List<RowImpl> before;
	private final List<RowImpl> after;

	PageAreaContent(PageAreaBuilderImpl pab, BlockContext bc) {
		 // FIXME: don't set pageShape to null
		bc = BlockContext.from(bc).pageShape(null).build();
		if (pab !=null) {
			//Assumes before is static
			this.before = Collections.unmodifiableList(renderRows(pab.getBeforeArea(), bc));

			//Assumes after is static
			this.after = Collections.unmodifiableList(renderRows(pab.getAfterArea(), bc));
		} else {
			this.before = Collections.emptyList();
			this.after = Collections.emptyList();
		}
	}

	private static List<RowImpl> renderRows(Iterable<Block> blocks, BlockContext bc) {
		List<RowImpl> ret = new ArrayList<>();
		for (Block b : blocks) {
			AbstractBlockContentManager bcm = b.getBlockContentManager(bc);
			Optional<RowImpl> r;
			// block context is independent of position (pageShape was set to null above)
			while ((r=bcm.getNext(-1)).isPresent()) {
				ret.add(r.get());
			}
		}
		return ret;
	}
	
	// FIXME: what if the returned rows do not fit on the page?
	// --> pass position
	List<RowImpl> getBefore() {
		return before;
	}

	// FIXME: what if the returned rows do not fit on the page?
	// --> pass position
	List<RowImpl> getAfter() {
		return after;
	}

}
