package org.daisy.dotify.formatter.impl.segment;

import org.daisy.dotify.api.formatter.DynamicContent;
import org.daisy.dotify.api.formatter.TextProperties;


/**
 * Provides an evaluate event object.
 * 
 * @author Joel Håkansson
 *
 */
public class Evaluate extends SegmentBase {
	private final DynamicContent expression;
	private final TextProperties props;
	
	/**
	 * @param expression the expression
	 * @param props the text properties
	 */
	public Evaluate(DynamicContent expression, TextProperties props) {
		this(expression, props, null);
	}

	public Evaluate(DynamicContent expression, TextProperties props, MarkerValue marker) {
		super(marker);
		this.expression = expression;
		this.props = props;
	}
	
	public DynamicContent getExpression() {
		return expression;
	}

	public TextProperties getTextProperties() {
		return props;
	}

	@Override
	public SegmentType getSegmentType() {
		return SegmentType.Evaluate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((props == null) ? 0 : props.hashCode());
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
		Evaluate other = (Evaluate) obj;
		if (expression == null) {
			if (other.expression != null) {
				return false;
			}
		} else if (!expression.equals(other.expression)) {
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
