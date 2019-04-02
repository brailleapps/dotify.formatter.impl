package org.daisy.dotify.formatter.impl.row;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.daisy.dotify.api.formatter.Context;
import org.daisy.dotify.api.formatter.DynamicContent;
import org.daisy.dotify.api.formatter.NumeralStyle;
import org.daisy.dotify.api.formatter.TextProperties;
import org.daisy.dotify.api.translator.MarkerProcessor;
import org.daisy.dotify.formatter.impl.segment.Evaluate;
import org.daisy.dotify.formatter.impl.segment.MarkerValue;
import org.daisy.dotify.formatter.impl.segment.PageNumberReference;
import org.daisy.dotify.formatter.impl.segment.Segment;
import org.daisy.dotify.formatter.impl.segment.Style;
import org.daisy.dotify.formatter.impl.segment.TextSegment;
import org.daisy.dotify.translator.DefaultMarkerProcessor;
import org.daisy.dotify.translator.Marker;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("javadoc")
public class SegmentProcessorTest {

	@Test
	public void testTextNoProcessor() {
		Context context = Mockito.mock(Context.class);
		Segment t;
		TextProperties tp = new TextProperties.Builder("und").build();
		List<Segment> segments = new ArrayList<>();
		List<Segment> expecteds = new ArrayList<>();
		t = new TextSegment("abc", tp);
		segments.add(t);
		expecteds.add(t);
		Style s = new Style("em");
		segments.add(s);
		t = new TextSegment("def", tp);
		s.add(t);
		expecteds.add(t);
		t = new TextSegment("ghi", tp);
		segments.add(t);
		expecteds.add(t);
		List<Segment> actuals = SegmentProcessor.processStyles(segments, null, context);
		assertEquals(expecteds, actuals);
	}
	
	@Test
	public void testTextWithProcessor() {
		Context context = Mockito.mock(Context.class);
		Segment t;
		TextProperties tp = new TextProperties.Builder("und").build();
		List<Segment> segments = new ArrayList<>();
		List<Segment> expecteds = new ArrayList<>();
		t = new TextSegment("abc", tp);
		segments.add(t);
		expecteds.add(t);
		Style s = new Style("em");
		segments.add(s);
		t = new TextSegment("def", tp);
		s.add(t);
		expecteds.add(new TextSegment("xdefy", tp));
		t = new TextSegment("ghi", tp);
		segments.add(t);
		expecteds.add(t);
		MarkerProcessor mp = new DefaultMarkerProcessor.Builder().addDictionary("em", (str, ta)->new Marker("x", "y")).build();
		List<Segment> actuals = SegmentProcessor.processStyles(segments, mp, context);
		assertEquals(expecteds, actuals);
	}
	
	@Test
	public void testDynamicWithProcessor_01() {
		Context context = Mockito.mock(Context.class);
		Segment t;
		TextProperties tp = new TextProperties.Builder("und").build();
		List<Segment> segments = new ArrayList<>();
		List<Segment> expecteds = new ArrayList<>();
		t = new TextSegment("abc", tp);
		segments.add(t);
		expecteds.add(t);
		Style s = new Style("em");
		segments.add(s);
		DynamicContent dc = Mockito.mock(DynamicContent.class);
		t = new Evaluate(dc, tp);
		s.add(t);
		expecteds.add(new Evaluate(dc, tp, new MarkerValue("x", "y")));
		t = new TextSegment("ghi", tp);
		segments.add(t);
		expecteds.add(t);
		MarkerProcessor mp = new DefaultMarkerProcessor.Builder().addDictionary("em", (str, ta)->new Marker("x", "y")).build();
		List<Segment> actuals = SegmentProcessor.processStyles(segments, mp, context);
		assertEquals(expecteds, actuals);
	}
	
	@Test
	public void testDynamicWithProcessor_02() {
		Context context = Mockito.mock(Context.class);
		Segment t;
		TextProperties tp = new TextProperties.Builder("und").build();
		List<Segment> segments = new ArrayList<>();
		List<Segment> expecteds = new ArrayList<>();
		t = new TextSegment("abc", tp);
		segments.add(t);
		expecteds.add(t);
		Style s = new Style("em");
		Style s1 = new Style("strong");
		segments.add(s);
		s.add(s1);
		DynamicContent dc = Mockito.mock(DynamicContent.class);
		t = new Evaluate(dc, tp);
		s1.add(t);
		expecteds.add(new Evaluate(dc, tp, new MarkerValue("x6", "7")));
		t = new TextSegment("ghi", tp);
		s.add(t);
		expecteds.add(new TextSegment("ghiy", tp));
		MarkerProcessor mp = new DefaultMarkerProcessor.Builder()
				.addDictionary("em", (str, ta)->new Marker("x", "y"))
				.addDictionary("strong", (str, ta)->new Marker("6", "7"))
				.build();
		List<Segment> actuals = SegmentProcessor.processStyles(segments, mp, context);
		assertEquals(expecteds, actuals);
	}
	
	@Test
	public void testDynamicWithProcessor_03() {
		Context context = Mockito.mock(Context.class);
		Segment t;
		TextProperties tp = new TextProperties.Builder("und").build();
		List<Segment> segments = new ArrayList<>();
		List<Segment> expecteds = new ArrayList<>();
		t = new TextSegment("abc", tp);
		segments.add(t);
		expecteds.add(t);
		Style s = new Style("em");
		Style s1 = new Style("strong");
		segments.add(s);
		s.add(s1);
		DynamicContent dc = Mockito.mock(DynamicContent.class);
		t = new PageNumberReference("id", NumeralStyle.ALPHA);
		s1.add(t);
		expecteds.add(new PageNumberReference("id", NumeralStyle.ALPHA, new MarkerValue("x6", "7")));
		t = new TextSegment("ghi", tp);
		s.add(t);
		expecteds.add(new TextSegment("ghiy", tp));
		MarkerProcessor mp = new DefaultMarkerProcessor.Builder()
				.addDictionary("em", (str, ta)->new Marker("x", "y"))
				.addDictionary("strong", (str, ta)->new Marker("6", "7"))
				.build();
		List<Segment> actuals = SegmentProcessor.processStyles(segments, mp, context);
		assertEquals(expecteds, actuals);
	}
}
