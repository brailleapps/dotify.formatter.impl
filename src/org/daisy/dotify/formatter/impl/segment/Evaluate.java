package org.daisy.dotify.formatter.impl.segment;

import org.daisy.dotify.api.formatter.DynamicContent;
import org.daisy.dotify.api.formatter.TextProperties;


/**
 * Provides an evaluate event object.
 * 
 * @author Joel Håkansson
 *
 */
public class Evaluate implements Segment {
	private final DynamicContent expression;
	private final TextProperties props;
	private final MarkerValue marker;
	
	/**
	 * @param expression the expression
	 * @param props the text properties
	 */
	public Evaluate(DynamicContent expression, TextProperties props) {
		this(expression, props, null);
	}

	public Evaluate(DynamicContent expression, TextProperties props, MarkerValue marker) {
		this.expression = expression;
		this.props = props;
		this.marker = marker;
	}
	
	public DynamicContent getExpression() {
		return expression;
	}

	public TextProperties getTextProperties() {
		return props;
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
	public SegmentType getSegmentType() {
		return SegmentType.Evaluate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((marker == null) ? 0 : marker.hashCode());
		result = prime * result + ((props == null) ? 0 : props.hashCode());
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
		Evaluate other = (Evaluate) obj;
		if (expression == null) {
			if (other.expression != null) {
				return false;
			}
		} else if (!expression.equals(other.expression)) {
			return false;
		}
		if (marker == null) {
			if (other.marker != null) {
				return false;
			}
		} else if (!marker.equals(other.marker)) {
			return false;
		}
		if (props == null) {
			if (other.props != null) {
				return false;
			}
		} else if (!props.equals(other.props)) {
			return false;
		}
		return true;
	}

	
}
