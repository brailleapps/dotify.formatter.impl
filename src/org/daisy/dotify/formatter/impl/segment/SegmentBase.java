package org.daisy.dotify.formatter.impl.segment;

public abstract class SegmentBase implements Segment {
	private final MarkerValue marker;
	
	public SegmentBase(MarkerValue marker) {
		this.marker = marker;
	}

	public String applyMarker(String exp) {
		if (marker!=null) {
			StringBuilder sb = new StringBuilder();
			if (marker.getPrefix()!=null) {
				sb.append(marker.getPrefix());
			}
			sb.append(exp);
			if (marker.getPostfix()!=null) {
				sb.append(marker.getPostfix());
			}
			return sb.toString();
		} else {
			return exp;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((marker == null) ? 0 : marker.hashCode());
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
		SegmentBase other = (SegmentBase) obj;
		if (marker == null) {
			if (other.marker != null) {
				return false;
			}
		} else if (!marker.equals(other.marker)) {
			return false;
		}
		return true;
	}

}
