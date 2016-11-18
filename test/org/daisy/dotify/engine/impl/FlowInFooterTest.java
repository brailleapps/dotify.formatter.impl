package org.daisy.dotify.engine.impl;

import java.io.IOException;

import org.daisy.dotify.api.engine.LayoutEngineException;
import org.daisy.dotify.api.writer.PagedMediaWriterConfigurationException;

import org.junit.Ignore;
import org.junit.Test;

public class FlowInFooterTest extends AbstractFormatterEngineTest {
	
	@Test
	public void testFlowInFooter1() throws LayoutEngineException, IOException, PagedMediaWriterConfigurationException {
		testPEF("resource-files/flow-in-footer-1-input.obfl",
		        "resource-files/flow-in-footer-1-expected.pef",
		        true);
	}
}
