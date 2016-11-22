package org.daisy.dotify.formatter.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.daisy.dotify.api.formatter.BlockPosition;
import org.daisy.dotify.api.formatter.RenderingScenario;

class PageSequenceRecorder {
	private static final String base = "base";
	private static final String best = "best";
	
	private PageSequenceRecorderData data;

	private RenderingScenario current = null;
	private boolean invalid = false;
	private double cost = 0;
	private float height = 0;
	private float minWidth = 0;
	private double forceCount = 0;
	private Map<String, PageSequenceRecorderData> states;

	PageSequenceRecorder() {
		data = new PageSequenceRecorderData();
		states = new HashMap<>();
	}
	
	private void saveState(String id) {
		states.put(id, new PageSequenceRecorderData(data));
	}
	
	private void restoreState(String id) {
		PageSequenceRecorderData state = states.get(id);
		if (state!=null) {
			data = new PageSequenceRecorderData(state);
		}
	}
	
	private void clearState(String id) {
		states.remove(id);
	}
	
	private boolean hasState(String id) {
		return states.containsKey(id);
	}
	
	void startScenario(RenderingScenario scenario) {
		if (current == scenario) {
			throw new IllegalStateException();
		} else if (current == null) {
			height = data.calcSize();
			cost = Double.MAX_VALUE;
			clearState(best);
			saveState(base);
		} else {
			if (!invalid) {
				//TODO: measure, evaluate
				float size = data.calcSize()-height;
				double ncost = current.calculateCost(setParams(size, minWidth, forceCount));
				if (ncost<cost) {
					//if better, store
					cost = ncost;
					saveState(best);
				}
			}
			restoreState(base);
		}
		forceCount = 0;
		minWidth = Float.MAX_VALUE;
		current = scenario;
		invalid = false;
	}

	void endScenarios() {
		if (current == null) {
			throw new IllegalStateException();
		}
		if (invalid) {
			if (hasState(best)) {
				restoreState(best);
			} else {
				throw new RuntimeException("Failed to render any scenario.");
			}
		} else {
			//if not better
			float size = data.calcSize()-height;
			double ncost = current.calculateCost(setParams(size, minWidth, forceCount));
			if (ncost>cost) {
				restoreState(best);
			}
		}
		current = null;
	}
	
	/**
	 * Invalidates the current scenario, if any. This causes the remainder of the
	 * scenario to be excluded from further processing.
	 * 
	 * @param e the exception that caused the scenario to be invalidated
	 * @throws RuntimeException if no scenario is active 
	 */
	void invalidateScenario(Exception e) {
		if (current == null) {
			throw new IllegalStateException();
		}
		invalid = true;
	}
	
	/**
	 * Process a new block for a scenario
	 * @param g
	 * @param rec
	 */
	void processBlock(AbstractBlockContentManager bcm) {
		if (current != null) {
			if (invalid) {
				return;
			}
			forceCount += bcm.getForceBreakCount();
			minWidth = Math.min(minWidth, bcm.getMinimumAvailableWidth());
		}
	}
	
	void newRowGroupSequence(BlockPosition pos, RowImpl emptyRow) {
		data.newRowGroupSequence(pos, emptyRow);
	}
	
	boolean isDataGroupsEmpty() {
		return data.dataGroups.isEmpty();
	}
	
	boolean isDataEmpty() {
		return data.isDataEmpty();
	}
	
	RowGroupSequence currentSequence() {
		return data.dataGroups.peek();
	}
	
	void addRowGroup(RowGroup rg) {
		data.addRowGroup(rg); 
	}
	
	List<RowGroupSequence> getResult() {
		if (current != null) {
			throw new IllegalStateException();
		}
		return data.dataGroups;
	}
	
	int getKeepWithNext() {
		return data.keepWithNext;
	}
	
	void setKeepWithNext(int keepWithNext) {
		data.keepWithNext = keepWithNext;
	}

	private static Map<String, Double> setParams(double height, double minBlockWidth, double forceCount) {
		Map<String, Double> params = new HashMap<>();
		params.put("total-height", height);
		params.put("min-block-width", minBlockWidth);
		params.put("forced-break-count", forceCount);
		return params;
	}

	private static class PageSequenceRecorderData {
		private Stack<RowGroupSequence> dataGroups = new Stack<>();
		private int keepWithNext = 0;

		PageSequenceRecorderData() {
			dataGroups = new Stack<>();
			keepWithNext = 0;
		}

		/**
		 * Creates a deep copy of the supplied instance
		 * @param template the instance to copy
		 */
		PageSequenceRecorderData(PageSequenceRecorderData template) {
			dataGroups = new Stack<>();
			for (RowGroupSequence rgs : template.dataGroups) {
				dataGroups.add(new RowGroupSequence(rgs));
			}
			keepWithNext = template.keepWithNext;
		}

		float calcSize() {
			float size = 0;
			for (RowGroupSequence rgs : dataGroups) {
				size += rgs.calcSequenceSize();
			}
			return size;
		}
		
		boolean isDataEmpty() {
			return (dataGroups.isEmpty()||dataGroups.peek().getGroup().isEmpty());
		}
		
		void newRowGroupSequence(BlockPosition pos, RowImpl emptyRow) {
			RowGroupSequence rgs = new RowGroupSequence(pos, emptyRow);
			dataGroups.add(rgs);
		}
		
		void addRowGroup(RowGroup rg) {
			dataGroups.peek().getGroup().add(rg);
		}

	}
}
