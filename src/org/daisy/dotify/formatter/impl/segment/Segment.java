package org.daisy.dotify.formatter.impl.segment;

import org.daisy.dotify.api.translator.ResolvableText;

public interface Segment extends ResolvableText {
	//{PCDATA, LEADER, MARKER, ANCHOR, BR, EVALUATE, BLOCK, TOC_ENTRY, PAGE_NUMBER}
	enum SegmentType {Text, NewLine, Leader, Reference, Marker, Anchor, Identifier, Evaluate, Style};
	
	public SegmentType getSegmentType();

}