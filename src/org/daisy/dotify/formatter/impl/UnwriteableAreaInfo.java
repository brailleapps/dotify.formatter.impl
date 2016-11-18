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
	
	// Note: not using LookupHandlers because I have a slightly different use
	// case: I need an empty map at the start of every pass and a copy of the
	// map at the end of the previous pass
	
	private Map<Position,UnwriteableArea> prvMap = null;
	
	public UnwriteableArea getUnwriteableArea(Block block, int positionInBlock) {
		if (prvMap == null) {
			return null;
		} else {
			return prvMap.get(new Position(block, positionInBlock));
		}
	}
	
	private Map<Position,UnwriteableArea> map = new HashMap<>();
	
	public void setUnwriteableArea(Block block, int positionInBlock, UnwriteableArea area) {
		if (block == null || area == null) {
			throw new IllegalArgumentException();
		}
		map.put(new Position(block, positionInBlock), area);
	}
	
	public boolean isDirty() {
		if (prvMap == null) {
			return !map.isEmpty();
		} else {
			return !map.equals(prvMap);
		}
	}
	
	public void commit() {
		prvMap = map;
		map = new HashMap<>();
	}
}
