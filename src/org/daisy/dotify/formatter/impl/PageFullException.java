package org.daisy.dotify.formatter.impl;

public class PageFullException extends Exception {
	
	private static final long serialVersionUID = -463645830012511731L;
	
	private final int effectiveFlowHeight;
	private final boolean headerRowTooShort;
	
	public PageFullException(int effectiveFlowHeight, boolean headerRowTooShort) {
		this.effectiveFlowHeight = effectiveFlowHeight;
		this.headerRowTooShort = headerRowTooShort;
	}
	
	public int getEffectiveFlowHeight() {
		return effectiveFlowHeight;
	}
	
	public boolean isHeaderRowTooShort() {
		return headerRowTooShort;
	}
}
