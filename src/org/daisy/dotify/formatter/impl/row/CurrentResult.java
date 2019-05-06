package org.daisy.dotify.formatter.impl.row;

import java.util.Optional;

/*
Q: What is this interface for? The resulting rows when the current rowgroup of a segment is processed?
*/
interface CurrentResult {
	boolean hasNext(SegmentProcessing spi);
	Optional<RowImpl> process(SegmentProcessing spi, LineProperties lineProps);
	CurrentResult copy();
}