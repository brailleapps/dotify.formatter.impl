package org.daisy.dotify.formatter.impl;

import java.util.ArrayList;
import java.util.List;

import org.daisy.dotify.api.formatter.BlockPosition;

class RowGroupSequence implements Cloneable {
	private List<RowGroup> group;
	private final BlockPosition pos;
	private RowImpl emptyRow;

	public RowGroupSequence(BlockPosition pos, RowImpl emptyRow) {
		this.group = new ArrayList<RowGroup>();
		this.pos = pos;
		this.emptyRow = emptyRow;
	}

	public List<RowGroup> getGroup() {
		return group;
	}
	
	RowGroup currentGroup() {
		if (group.isEmpty()) {
			return null;
		} else {
			return group.get(group.size()-1);
		}
	}

	public BlockPosition getBlockPosition() {
		return pos;
	}

	public RowImpl getEmptyRow() {
		return emptyRow;
	}
	
	float calcSequenceSize() {
		float ret = 0;
		for (RowGroup rg : getGroup()) {
			ret += rg.getUnitSize();
		}
		return ret;
	}

	/**
	 * Creates a deep copy
	 */
	public Object clone() {
		RowGroupSequence clone; {
			try {
				clone = (RowGroupSequence)super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
		}
		clone.group = new ArrayList<>(); {
			for (RowGroup rg : this.group) {
				clone.group.add(new RowGroup(rg));
			}
		}
		clone.emptyRow = new RowImpl(this.emptyRow);
		return clone;
	}
}
