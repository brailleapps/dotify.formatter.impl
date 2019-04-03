package org.daisy.dotify.formatter.impl.segment;

import org.daisy.dotify.api.formatter.NumeralStyle;


/**
 * Provides a page number reference event object.
 * 
 * @author Joel Håkansson
 */
public class PageNumberReference extends SegmentBase {
	private final String refid;
	private final NumeralStyle style;
	
	public PageNumberReference(String refid, NumeralStyle style) {
		this(refid, style, null);
	}
	
	public PageNumberReference(String refid, NumeralStyle style, MarkerValue marker) {
		super(marker);
		this.refid = refid;
		this.style = style;
	}
	
	/**
	 * Gets the identifier to the reference location.
	 * @return returns the reference identifier
	 */
	public String getRefId() {
		return refid;
	}
	
	/**
	 * Gets the numeral style for this page number reference
	 * @return returns the numeral style
	 */
	public NumeralStyle getNumeralStyle() {
		return style;
	}
	
	@Override
	public SegmentType getSegmentType() {
		return SegmentType.Reference;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((refid == null) ? 0 : refid.hashCode());
		result = prime * result + ((style == null) ? 0 : style.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PageNumberReference other = (PageNumberReference) obj;
		if (refid == null) {
			if (other.refid != null) {
				return false;
			}
		} else if (!refid.equals(other.refid)) {
			return false;
		}
		if (style != other.style) {
			return false;
		}
		return true;
	}

}
