package org.daisy.dotify.formatter.impl;

import java.util.HashMap;
import java.util.Map;

public class UnwriteableAreaInfo {
	
	public static class UnwriteableArea {
		
		public enum Side {
			RIGHT,
			LEFT
		}
		
		public final Side side;
		public final int width;
		
		public UnwriteableArea(Side side, int width) {
			this.side = side;
			this.width = width;
		}
		
		@Override
		public String toString() {
			return "UnwriteableArea{side="+side+", width="+width+"}";
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + side.hashCode();
			result = prime * result + width;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			UnwriteableArea other = (UnwriteableArea)obj;
			if (side != other.side) {
				return false;
			}
			if (width != other.width) {
				return false;
			}
			return true;
		}
	}
	
	private static class Position {
		
		final Block block;
		final int index;
		
		Position(Block block, int index) {
			this.block = block;
			this.index = index;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + block.hashCode();
			result = prime * result + index;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Position other = (Position)obj;
			if (block != other.block) {
				return false;
			}
			if (index != other.index) {
				return false;
			}
			return true;
		}
	}
	
	// Note: not using LookupHandler because I have a slightly different use case.
	
	private Map<Position,UnwriteableArea> map = new HashMap<>();
	private Map<Position,UnwriteableArea> uncommitted = new HashMap<>();
	private Map<Position,UnwriteableArea> beforeMark = new HashMap<>();
	private boolean dirty = false;
	
	public UnwriteableArea getUnwriteableArea(Block block, int positionInBlock) {
		return map.get(new Position(block, positionInBlock));
	}
	
	public void setUnwriteableArea(Block block, int positionInBlock, UnwriteableArea area) {
		if (block == null || area == null) {
			throw new IllegalArgumentException("null");
		}
		Position pos = new Position(block, positionInBlock);
		if (beforeMark.containsKey(pos) || uncommitted.put(pos, area) != null) {
			throw new IllegalStateException();
		}
		if (!dirty && !area.equals(map.get(pos))) {
			dirty = true;
		}
	}
	
	public boolean isDirty() {
		if (!dirty && uncommitted.size() < map.size()) {
			dirty = true;
		}
		return dirty;
	}
	
	public void commit() {
		map.clear();
		map.putAll(uncommitted);
		uncommitted.clear();
		dirty = false;
	}
	
	public void mark() {
		if (!uncommitted.isEmpty()) {
			throw new IllegalStateException("uncommitted values");
		}
		beforeMark.putAll(map);
		map.clear();
	}
	
	public void reset() {
		uncommitted.clear();
		dirty = false;
	}
	
	public void rewind() {
		uncommitted.clear();
		dirty = false;
		map.putAll(beforeMark);
		beforeMark.clear();
	}
}
