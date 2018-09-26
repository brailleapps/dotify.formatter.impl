package org.daisy.dotify.formatter.impl.search;

import java.util.List;
import java.util.Optional;

import org.daisy.dotify.api.formatter.Marker;
import org.daisy.dotify.api.formatter.MarkerReferenceField;
import org.daisy.dotify.api.formatter.MarkerReferenceField.MarkerSearchDirection;
import org.daisy.dotify.api.formatter.MarkerReferenceField.MarkerSearchScope;
import org.daisy.dotify.common.collection.ImmutableList;
import org.daisy.dotify.common.collection.ImmutableMap;

class SearchInfo implements Cloneable {

	private ImmutableMap<DocumentSpace, DocumentSpaceData> spaces;
	private Runnable setDirty;
	
	SearchInfo(Runnable setDirty) {
		this.spaces = ImmutableMap.empty();
		this.setDirty = setDirty;
	}
	
	// FIXME: move to Builder and/or return new SearchInfo object?
	// -> in this case clone method can also be removed
	void setPageDetails(PageDetails value) {
		DocumentSpace space = value.getSequenceId().getSpace();
		DocumentSpaceData data = getViewForSpace(space).clone();
		ImmutableList.Builder<PageDetails> pageDetails = data.pageDetails.builder();
		while (value.getPageId().getPageIndex()>=pageDetails.size()) {
			pageDetails.add(null);
		}
		PageDetails old = pageDetails.set(value.getPageId().getPageIndex(), value);
		// FIXME: Only check the previous value if dirty isn't already true
		if (!value.equals(old)) {
			setDirty.run();
		}
		data.pageDetails = pageDetails.build();
		spaces = ImmutableMap.put(spaces, space, data);
	}

	View<PageDetails> getPageView(DocumentSpace space) {
		return new View<PageDetails>(getViewForSpace(space).pageDetails, 0, getViewForSpace(space).pageDetails.size());
	}

	View<PageDetails> getContentsInVolume(int volumeNumber, DocumentSpace space) {
		return getViewForSpace(space).volumeViews.get(volumeNumber);
	}
	
	View<PageDetails> getContentsInSequence(SequenceId seqId) {
		return getViewForSpace(seqId.getSpace()).sequenceViews.get(seqId.getOrdinal());
	}
	
	// FIXME: move to Builder and/or return new SearchInfo object?
	// -> in this case clone method can also be removed
	void setSequenceScope(DocumentSpace space, int sequenceNumber, int fromIndex, int toIndex) {
		DocumentSpaceData data = getViewForSpace(space).clone();
		View<PageDetails> pw = new View<PageDetails>(data.pageDetails, fromIndex, toIndex);
		data.sequenceViews = ImmutableMap.put(data.sequenceViews, sequenceNumber, pw);
		spaces = ImmutableMap.put(spaces, space, data);
	}
	
	// FIXME: move to Builder and/or return new SearchInfo object?
	// -> in this case clone method can also be removed
	void setVolumeScope(int volumeNumber, int fromIndex, int toIndex) {
		setVolumeScope(volumeNumber, fromIndex, toIndex, DocumentSpace.BODY);
	}

	// FIXME: move to Builder and/or return new SearchInfo object?
	// -> in this case clone method can also be removed
	void setVolumeScope(int volumeNumber, int fromIndex, int toIndex, DocumentSpace space) {
		DocumentSpaceData data = getViewForSpace(space).clone();
		View<PageDetails> pw = new View<PageDetails>(data.pageDetails, fromIndex, toIndex);
		for (PageDetails p : pw.getItems()) {
			p.setVolumeNumber(volumeNumber);
		}
		data.volumeViews = ImmutableMap.put(data.volumeViews, volumeNumber, pw);
		spaces = ImmutableMap.put(spaces, space, data);
	}
	
	DocumentSpaceData getViewForSpace(DocumentSpace space) {
		DocumentSpaceData ret = spaces.get(space);
		if (ret==null) {
			ret = new DocumentSpaceData();
			spaces = ImmutableMap.put(spaces, space, ret);
		}
		return ret;
	}
	
	PageDetails getPageInSequenceWithOffset(PageDetails base, int offset, boolean adjustOutOfBounds) {
		return offset==0?base:base.getPageInScope(getContentsInSequence(base.getSequenceId()), offset, adjustOutOfBounds);
	}
	
	PageDetails getPageInDocumentWithOffset(PageDetails base, int offset, boolean adjustOutOfBounds) {
		if (offset==0) {
			return base;
		} else {
			//Keep while moving: getPageInScope(base.getSequenceParent().getParent().getPageView()...
			return base.getPageInScope(getPageView(base.getSequenceId().getSpace()), offset, adjustOutOfBounds);
		}
	}
	
	PageDetails getPageInVolumeWithOffset(PageDetails base, int offset, boolean adjustOutOfBounds) {
		if (offset==0) {
			return base;
		} else {
			//Keep while moving: base.getPageInScope(base.getSequenceParent().getParent().getContentsInVolume(base.getVolumeNumber()), offset, adjustOutOfBounds);
			return base.getPageInScope(getContentsInVolume(base.getVolumeNumber(), base.getSequenceId().getSpace()), offset, adjustOutOfBounds);
		}
	}
	
	boolean isWithinVolumeSpreadScope(PageDetails base, int offset) {
		if (offset==0) {
			return true;
		} else {
			PageDetails n = getPageInVolumeWithOffset(base, offset, false);
			return base.isWithinSpreadScope(offset, n);
		}
	}
	
	/*
	 * This method is unused at the moment, but could be activated if additional scopes are added to the API,
	 * namely SPREAD_WITHIN_DOCUMENT
	 */
	boolean isWithinDocumentSpreadScope(PageDetails base, int offset) {
		if (offset==0) {
			return true;
		} else {
			PageDetails n = getPageInDocumentWithOffset(base, offset, false);
			return base.isWithinSpreadScope(offset, n);
		}
	}
	
	boolean shouldAdjustOutOfBounds(PageDetails base, MarkerReferenceField markerRef) {
		if (markerRef.getSearchDirection()==MarkerSearchDirection.FORWARD && markerRef.getOffset()>=0 ||
			markerRef.getSearchDirection()==MarkerSearchDirection.BACKWARD && markerRef.getOffset()<=0) {
			return false;
		} else {
			switch(markerRef.getSearchScope()) {
			case PAGE_CONTENT: case PAGE:
				return false;
			case SEQUENCE: case VOLUME: case DOCUMENT:
				return true;
			case SPREAD_CONTENT: case SPREAD:
				//return  isWithinSequenceSpreadScope(markerRef.getOffset());				
				//return  isWithinDocumentSpreadScope(markerRef.getOffset());
				return isWithinVolumeSpreadScope(base, markerRef.getOffset());
			case SHEET:
				return base.isWithinSheetScope(markerRef.getOffset()) && 
						markerRef.getSearchDirection()==MarkerSearchDirection.BACKWARD;
			default:
				throw new RuntimeException("Error in code. Missing implementation for value: " + markerRef.getSearchScope());
			}
		}
	}
	
	String findMarker(PageDetails page, MarkerReferenceField markerRef) {
		if (page==null) {
			return "";
		}
		if (markerRef.getSearchScope()==MarkerSearchScope.VOLUME || markerRef.getSearchScope()==MarkerSearchScope.DOCUMENT) {
			throw new RuntimeException("Marker reference scope not implemented: " + markerRef.getSearchScope());
		}
		int dir = 1;
		int index = 0;
		int count = 0;
		List<Marker> m;
		boolean skipLeading = false;
		if (markerRef.getSearchScope() == MarkerReferenceField.MarkerSearchScope.PAGE_CONTENT) {
			skipLeading = true;
		} else if (markerRef.getSearchScope() == MarkerReferenceField.MarkerSearchScope.SPREAD_CONTENT) {
			PageDetails prevPageInVolume = getPageInVolumeWithOffset(page, -1, false);
			if (prevPageInVolume == null || !page.isWithinSpreadScope(-1, prevPageInVolume)) {
				skipLeading = true;
			}
		}
		if (skipLeading) {
			m = page.getContentMarkers();
		} else {
			m = page.getMarkers();
		}
		if (markerRef.getSearchDirection() == MarkerReferenceField.MarkerSearchDirection.BACKWARD) {
			dir = -1;
			index = m.size()-1;
		}
		while (count < m.size()) {
			Marker m2 = m.get(index);
			if (m2.getName().equals(markerRef.getName())) {
				return m2.getValue();
			}
			index += dir; 
			count++;
		}
		PageDetails next = null;
		if (markerRef.getSearchScope() == MarkerReferenceField.MarkerSearchScope.SEQUENCE ||
			markerRef.getSearchScope() == MarkerSearchScope.SHEET && page.isWithinSheetScope(dir) //||
			//markerRef.getSearchScope() == MarkerSearchScope.SPREAD && page.isWithinSequenceSpreadScope(dir)
			) {
			//Keep while moving: next = page.getPageInScope(page.getSequenceParent(), dir, false);
			next = page.getPageInScope(getContentsInSequence(page.getSequenceId()), dir, false);
		} //else if (markerRef.getSearchScope() == MarkerSearchScope.SPREAD && page.isWithinDocumentSpreadScope(dir)) {
		  else if (markerRef.getSearchScope() == MarkerSearchScope.SPREAD ||
		           markerRef.getSearchScope() == MarkerSearchScope.SPREAD_CONTENT) {
			if (isWithinVolumeSpreadScope(page, dir)) {
				next = getPageInVolumeWithOffset(page, dir, false);
			}
		}
		if (next!=null) {
			return findMarker(next, markerRef);
		} else {
			return "";
		}
	}
	
	private Optional<PageDetails> getPageDetails(PageId p) {
		DocumentSpaceData data = getViewForSpace(p.getSequenceId().getSpace());
		if (p.getPageIndex()<data.pageDetails.size()) {
			return Optional.ofNullable(data.pageDetails.get(p.getPageIndex()));
		} else {
			return Optional.empty();
		}
	}
	
	String findStartAndMarker(PageId id, MarkerReferenceField f2) {
		return getPageDetails(id)
			.map(p->{
					PageDetails start;
					if (f2.getSearchScope()==MarkerSearchScope.SPREAD ||
						f2.getSearchScope()==MarkerSearchScope.SPREAD_CONTENT) {
						start = getPageInVolumeWithOffset(p, f2.getOffset(), shouldAdjustOutOfBounds(p, f2));
					} else {
						//Keep while moving: start = p.getPageInScope(p.getSequenceParent(), f2.getOffset(), shouldAdjustOutOfBounds(p, f2));
						start = p.getPageInScope(getContentsInSequence(p.getSequenceId()), f2.getOffset(), shouldAdjustOutOfBounds(p, f2));
					}
					return findMarker(start, f2);
				})
			.orElse("");
	}
	
	Optional<PageDetails> findNextPageInSequence(PageId id) {
		return getPageDetails(id).flatMap(p->Optional.ofNullable(getPageInSequenceWithOffset(p, 1, false)));
	}
	
	@Override
	public SearchInfo clone() {
		SearchInfo clone;
		try {
			clone = (SearchInfo)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError("coding error");
		}
		return clone;
	}
}
