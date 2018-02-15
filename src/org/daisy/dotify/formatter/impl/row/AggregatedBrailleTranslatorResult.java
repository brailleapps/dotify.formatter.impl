package org.daisy.dotify.formatter.impl.row;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.stream.Collectors;

import org.daisy.dotify.api.formatter.Marker;
import org.daisy.dotify.api.translator.BrailleTranslatorResult;
import org.daisy.dotify.api.translator.UnsupportedMetricException;
import org.daisy.dotify.formatter.impl.segment.AnchorSegment;
import org.daisy.dotify.formatter.impl.segment.IdentifierSegment;
import org.daisy.dotify.formatter.impl.segment.MarkerSegment;

/**
 * Provides an aggregated braille translator result.
 * @author Bert Frees
 * @author Joel Håkansson
 */
class AggregatedBrailleTranslatorResult implements BrailleTranslatorResult {
	
	private static final Pattern trailingWsBraillePattern = Pattern.compile("[\\s\u2800]+\\z");
	
	private final List<Object> results;
	private int currentIndex;
	private List<Marker> pendingMarkers;
	private List<String> pendingAnchors;
	private List<String> pendingIdentifiers;
	
	/**
	 * Provides a builder for an aggregated braille translator result.
	 */
	static class Builder {
		private final List<Object> results;
		Builder() {
			this.results = new ArrayList<>();
		}
		
		Builder(Builder template) {
			this.results = copyResults(template.results);
		}
		
		/**
		 * Adds a marker to the aggregated result.
		 * @param m the marker to add
		 */
		void add(MarkerSegment m) {
			results.add(m);
		}
		
		/**
		 * Adds an anchor segment to the aggregated result.
		 * @param as the anchor segement to add
		 */
		void add(AnchorSegment as) {
			results.add(as);
		}
		
		/**
		 * Adds an identifier segment to the aggregated result.
		 * @param as the identifier segment to add
		 */
		void add(IdentifierSegment is) {
			results.add(is);
		}
		
		/**
		 * Adds a braille translator result to the aggregated result.
		 * @param bts the translator result to add
		 */
		void add(BrailleTranslatorResult bts) {
			results.add(bts);
		}
		
		boolean isEmpty() {
			return results.isEmpty();
		}
		
		/**
		 * Builds an aggregated braille translator result based on the
		 * state of this builder.
		 * @return returns a new aggregated translator result
		 */
		AggregatedBrailleTranslatorResult build() {
			return new AggregatedBrailleTranslatorResult(this);
		}

	}
	
	private AggregatedBrailleTranslatorResult(Builder builder) {
		this.results = Collections.unmodifiableList(new ArrayList<>(builder.results));
		this.currentIndex = 0;
		this.pendingMarkers = new ArrayList<>();
		this.pendingAnchors = new ArrayList<>();
		this.pendingIdentifiers = new ArrayList<>();
	}
	
	private AggregatedBrailleTranslatorResult(AggregatedBrailleTranslatorResult template) {
		this.results = copyResults(template.results);
		this.currentIndex = template.currentIndex;
		this.pendingMarkers = new ArrayList<>(template.pendingMarkers);
		this.pendingAnchors = new ArrayList<>(template.pendingAnchors);
		this.pendingIdentifiers = new ArrayList<>(template.pendingIdentifiers);
	}
	
	private static List<Object> copyResults(List<?> inputs) {
		return inputs.stream().map(o->{
			if (o instanceof BrailleTranslatorResult) {
				BrailleTranslatorResult btr = ((BrailleTranslatorResult)o);
				return btr.copy();
			} else {
				return o;
			}
		}).collect(Collectors.toList());
	}

	private static class State {
		final int rowSize;
		final int currentIndex;
		final int pendingMarkersSize;
		final int pendingAnchorsSize;
		final int pendingIdentifiersSize;
		State(int rowSize, int currentIndex, int pendingMarkersSize, int pendingAnchorsSize, int pendingIdentifiersSize) {
			this.rowSize = rowSize;
			this.currentIndex = currentIndex;
			this.pendingMarkersSize = pendingMarkersSize;
			this.pendingAnchorsSize = pendingAnchorsSize;
			this.pendingIdentifiersSize = pendingIdentifiersSize;
		}
		static State save(AggregatedBrailleTranslatorResult btr, String row) {
			return new State(row.length(),
			                 btr.currentIndex,
			                 btr.pendingMarkers.size(),
			                 btr.pendingAnchors.size(),
			                 btr.pendingIdentifiers.size());
		}
		String restore(AggregatedBrailleTranslatorResult btr, String row) {
			row = row.substring(0, rowSize);
			btr.currentIndex = currentIndex;
			btr.pendingMarkers.subList(pendingMarkersSize, btr.pendingMarkers.size()).clear();
			btr.pendingAnchors.subList(pendingAnchorsSize, btr.pendingAnchors.size()).clear();
			btr.pendingIdentifiers.subList(pendingIdentifiersSize, btr.pendingIdentifiers.size()).clear();
			return row;
		}
	}

	private static void failIf(boolean test) {
		if (test) {
			throw new RuntimeException("coding error");
		}
	}

	@Override
	public String nextTranslatedRow(int limit, boolean force, boolean wholeWordsOnly) {
		String row = nextTranslatedRowInner(limit, false, wholeWordsOnly);
		if (force && row.isEmpty())
			row = nextTranslatedRowInner(limit, true, wholeWordsOnly);
		return row;
	}
	
	private String nextTranslatedRowInner(int limit, boolean force, boolean wholeWordsOnly) {
		String row = "";
		Stack<State> backup = new Stack<>();
		BrailleTranslatorResult current = computeNext();
		if (current == null)
			throw new NoSuchElementException();
		o: while (limit > row.length()) {
			failIf(current == null || !current.hasNext());
			if (current.countRemaining() < limit - row.length()) {
				String remainder = current.getTranslatedRemainder();
				failIf(remainder.isEmpty());
				backup.push(State.save(this, row));
				row += remainder;
				currentIndex++;
				current = computeNext();
				if (current == null) {
					break; // everything fits on a line; return
				} // there is more content; try fitting the next segment
			} else { // segment does not fit on the line, try breaking it
				while (true) {
					String r = current.nextTranslatedRow(limit - row.length(), force, wholeWordsOnly);
					if (!r.isEmpty()) {
						row += r;
						break o; // segment was broken; return
					} else if (!current.hasNext()) {
						// The BrailleTranslatorResult wrongly indicated that it had more rows. Not sure whether this is
						// an error? Just ignore for now.
						current = computeNext();
						failIf(current == null); // if everything would fit on a line we would have detected it already
						// there is more content, try breaking the next segment
					} else {
						if (force) {
							throw new RuntimeException("corrupt BrailleTranslatorResult: " + current + ": "
							                           + "hasNext() is true but nextTranslatedRow("+ (limit - row.length()) + ", true)"
							                           + " returns empty string");
						}
						if (backup.isEmpty()) {
							failIf(!row.isEmpty());
							break o; // return empty row
						} else {
							failIf(row.isEmpty());
							row = backup.pop().restore(this, row); // backup to the previous segment
							current = computeNext();
							failIf(current == null);
							// try breaking the previous segment
						}
					}
				}
			}
		}
		if (hasNext()) {
			row = trailingWsBraillePattern.matcher(row).replaceAll("");
		}
		return row;
	}

	private BrailleTranslatorResult computeNext() {
		while (currentIndex < results.size()) {
			Object o = results.get(currentIndex);
			if (o instanceof BrailleTranslatorResult) {
				BrailleTranslatorResult current = ((BrailleTranslatorResult)o);
				if (current.hasNext()) {
					return current;
				}
			} else if (o instanceof MarkerSegment) {
				pendingMarkers.add((MarkerSegment)o);
			} else if (o instanceof AnchorSegment) {
				pendingAnchors.add(((AnchorSegment)o).getReferenceID());
			} else if (o instanceof IdentifierSegment) {
				pendingIdentifiers.add(((IdentifierSegment)o).getName());
			} else {
				throw new RuntimeException("coding error");
			}
			currentIndex++;
		}
		return null;
	}

	@Override
	public String getTranslatedRemainder() {
		String remainder = "";
		for (int i = currentIndex; i < results.size(); i++) {
			Object o = results.get(i);
			if (o instanceof BrailleTranslatorResult) {
				remainder += ((BrailleTranslatorResult)o).getTranslatedRemainder();
			}
		}
		return remainder;
	}

	@Override
	public int countRemaining() {
		int remaining = 0;
		for (int i = currentIndex; i < results.size(); i++) {
			Object o = results.get(i);
			if (o instanceof BrailleTranslatorResult) {
				remaining += ((BrailleTranslatorResult)o).countRemaining();
			}
		}
		return remaining;
	}

	@Override
	public boolean hasNext() {
		return computeNext() != null;
	}
	
	List<Marker> getMarkers() {
		return Collections.unmodifiableList(pendingMarkers);
	}
	
	List<String> getAnchors() {
		return Collections.unmodifiableList(pendingAnchors);
	}
	
	List<String> getIdentifiers() {
		return Collections.unmodifiableList(pendingIdentifiers);
	}
	
	void clearPending() {
		pendingMarkers.clear();
		pendingAnchors.clear();
		pendingIdentifiers.clear();
	}

	@Override
	public boolean supportsMetric(String metric) {
		// since we cannot assume that the individual results of any metric can be added, we only support the following known cases
		if (METRIC_FORCED_BREAK.equals(metric) || METRIC_HYPHEN_COUNT.equals(metric)) {
			for (int i = 0; i <= currentIndex && i < results.size(); i++) {
				Object o = results.get(i);
				if (o instanceof BrailleTranslatorResult) {
					if (!((BrailleTranslatorResult)o).supportsMetric(metric)) {
						return false;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public double getMetric(String metric) {
		// since we cannot assume that the individual results of any metric can be added, we only support the following known cases
		if (METRIC_FORCED_BREAK.equals(metric) || METRIC_HYPHEN_COUNT.equals(metric)) {
			int count = 0;
			for (int i = 0; i <= currentIndex && i < results.size(); i++) {
				Object o = results.get(i);
				if (o instanceof BrailleTranslatorResult) {
					count += ((BrailleTranslatorResult)o).getMetric(metric);
				}
			}
			return count;
		} else {
			throw new UnsupportedMetricException("Metric not supported: " + metric);
		}
	}

	@Override
	public BrailleTranslatorResult copy() {
		return new AggregatedBrailleTranslatorResult(this);
	}

}
