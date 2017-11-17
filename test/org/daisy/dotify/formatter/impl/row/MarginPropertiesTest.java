package org.daisy.dotify.formatter.impl.row;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarginPropertiesTest {

	@Test
	public void testAssociative() {
		// (a op b) op c == a op (b op c)
		MarginProperties a = new MarginProperties(" a", false);
		MarginProperties b = new MarginProperties(" ", true);
		MarginProperties c = new MarginProperties("c ", false);
		MarginProperties abc = new MarginProperties(" a c ", false);
		assertEquals(abc, a.append(b).append(c));
		assertEquals(abc, a.append(b.append(c)));
	}
}

