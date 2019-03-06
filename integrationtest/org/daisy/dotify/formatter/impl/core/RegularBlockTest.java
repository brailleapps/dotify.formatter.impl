package org.daisy.dotify.formatter.impl.core;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.daisy.dotify.api.formatter.BlockProperties;
import org.daisy.dotify.api.formatter.Context;
import org.daisy.dotify.api.formatter.DynamicContent;
import org.daisy.dotify.api.formatter.FormatterConfiguration;
import org.daisy.dotify.api.formatter.TextProperties;
import org.daisy.dotify.api.translator.BrailleTranslatorFactoryMaker;
import org.daisy.dotify.api.translator.MarkerProcessor;
import org.daisy.dotify.api.translator.MarkerProcessorConfigurationException;
import org.daisy.dotify.api.translator.MarkerProcessorFactoryMakerService;
import org.daisy.dotify.api.translator.TextAttribute;
import org.daisy.dotify.api.translator.TranslatorConfigurationException;
import org.daisy.dotify.formatter.impl.row.AbstractBlockContentManager;
import org.daisy.dotify.formatter.impl.row.RowDataProperties;
import org.daisy.dotify.formatter.impl.search.DefaultContext;
import org.daisy.dotify.translator.DefaultMarkerProcessor;
import org.daisy.dotify.translator.Marker;
import org.junit.Test;
import org.mockito.Mockito;

public class RegularBlockTest {

	@Test
	public void testConnectedStyles() throws MarkerProcessorConfigurationException, TranslatorConfigurationException {
		String loc = "sv-SE";
		String mode = "bypass";
		TextProperties tp = new TextProperties.Builder(loc).hyphenate(false).build();
		DynamicContent exp = new DynamicContent() {
			String str = "b";
			@Override
			public String render(Context context) {
				return str;
			}
			
			@Override
			public String render() {
				return render(new Context() {});
			}
		};
	
		MarkerProcessorFactoryMakerService mpf = Mockito.mock(MarkerProcessorFactoryMakerService.class);
		MarkerProcessor mp = new DefaultMarkerProcessor.Builder()
				.addDictionary("em", (String str, TextAttribute attributes)->new Marker("1>", "<1"))
				.addDictionary("strong", (String str, TextAttribute attributes)->new Marker("2>", "<2"))
				.build();
		Mockito.when(mpf.newMarkerProcessor(loc, mode)).thenReturn(mp);

		FormatterContext fc = new FormatterContext(
				BrailleTranslatorFactoryMaker.newInstance(),
				null,
				mpf,
				new FormatterConfiguration.Builder(loc, mode).build());

		FormatterCoreImpl f = new FormatterCoreImpl(fc);
		f.startBlock(new BlockProperties.Builder().build());
		f.startStyle("em");
		f.addChars("a", tp);
		f.startStyle("strong");
		f.insertEvaluate(exp, tp);
		f.endStyle();
		f.addChars("c", tp);
		f.endStyle();
		f.endBlock();
		List<Block> b = f.getBlocks(null, null, null);
		Block bl = b.get(0);
		AbstractBlockContentManager bcm = bl.getBlockContentManager(new BlockContext.Builder(new DefaultContext.Builder(null).build()).flowWidth(30).formatterContext(fc).build());
		StringBuilder sb = new StringBuilder();
		while (bcm.hasNext()) {
			bcm.getNext().ifPresent(v->sb.append(v.getChars()));
		}
		assertEquals("1>a2>b<2c<1", sb.toString());
	}
}
