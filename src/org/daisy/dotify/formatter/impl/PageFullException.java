package org.daisy.dotify.formatter.impl;

public class PageFullException extends Exception {
	
	private static final long serialVersionUID = -463645830012511731L;
	
	private final int effectiveFlowHeight;
	
	public PageFullException(int effectiveFlowHeight) {
		this.effectiveFlowHeight = effectiveFlowHeight;
	}
	
	public int getEffectiveFlowHeight() {
		return effectiveFlowHeight;
	}
}
