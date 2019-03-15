package org.daisy.dotify.formatter.impl.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Style implements Segment {
	private final List<Segment> segments;
	private final String style;
	
	public Style(String style) {
		this.segments = new ArrayList<>();
		this.style = style;
	}
	
	public String getName() {
		return style;
	}
	
	/*
	 * @returns the index of segment inside the group
	 */
	public int add(Segment segment) {
		segments.add(segment);
		return segments.size()-1;
	}

	Segment getSegmentAt(int idx) {
		return (Segment)segments.get(idx);
	}
	
	public List<Segment> getSegments() {
		return Collections.unmodifiableList(segments);
	}
	
	@Override
	public SegmentType getSegmentType() {
		return SegmentType.Style;
	}
}
