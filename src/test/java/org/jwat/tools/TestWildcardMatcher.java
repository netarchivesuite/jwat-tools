package org.jwat.tools;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.tools.WildcardMatcher.WildcardPart;

@RunWith(JUnit4.class)
public class TestWildcardMatcher {

	@Test
	public void test_wildcard_matcher() {
		WildcardMatcher wm;
		WildcardPart part;

		wm = new WildcardMatcher("string");
		Assert.assertEquals(1, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("string", part.str);

		Assert.assertTrue(wm.match("string"));
		Assert.assertFalse(wm.match("tring"));
		Assert.assertFalse(wm.match("strin"));
		Assert.assertFalse(wm.match("strings"));

		wm = new WildcardMatcher("*.gz");
		Assert.assertEquals(2, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals(".gz", part.str);

		Assert.assertTrue(wm.match(".gz"));
		Assert.assertTrue(wm.match(".gz.gz.gz"));
		Assert.assertTrue(wm.match("test.gz"));
		Assert.assertFalse(wm.match(".g"));
		Assert.assertFalse(wm.match(".gz.gz.g"));
		Assert.assertFalse(wm.match("test.g"));
		Assert.assertFalse(wm.match(".gzip"));
		Assert.assertFalse(wm.match(".gz.gz.gzip"));
		Assert.assertFalse(wm.match("test.gzip"));

		wm = new WildcardMatcher("test*");
		Assert.assertEquals(2, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("test", part.str);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);

		Assert.assertTrue(wm.match("test"));
		Assert.assertTrue(wm.match("testtest"));
		Assert.assertFalse(wm.match("est"));
		Assert.assertFalse(wm.match("tes"));

		wm = new WildcardMatcher("*");
		Assert.assertEquals(1, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);

		Assert.assertTrue(wm.match("*"));
		Assert.assertTrue(wm.match("***"));

		wm = new WildcardMatcher("***");
		Assert.assertEquals(1, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);

		Assert.assertTrue(wm.match("t"));
		Assert.assertTrue(wm.match("test"));

		wm = new WildcardMatcher("***###");
		Assert.assertEquals(2, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(3, part.num);

		Assert.assertTrue(wm.match("1234"));
		Assert.assertTrue(wm.match("123"));
		Assert.assertFalse(wm.match("12"));
		Assert.assertFalse(wm.match("1"));

		wm = new WildcardMatcher("#");
		Assert.assertEquals(1, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(1, part.num);

		Assert.assertTrue(wm.match("1"));
		Assert.assertFalse(wm.match("12"));
		Assert.assertFalse(wm.match("123"));

		wm = new WildcardMatcher("###");
		Assert.assertEquals(1, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(3, part.num);

		Assert.assertTrue(wm.match("123"));
		Assert.assertFalse(wm.match("12"));
		Assert.assertFalse(wm.match("1"));

		wm = new WildcardMatcher("###***");
		Assert.assertEquals(2, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(3, part.num);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);

		Assert.assertTrue(wm.match("1234"));
		Assert.assertTrue(wm.match("123"));
		Assert.assertFalse(wm.match("12"));
		Assert.assertFalse(wm.match("1"));

		wm = new WildcardMatcher("test*monkey");
		Assert.assertEquals(3, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("test", part.str);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(2);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("monkey", part.str);

		Assert.assertTrue(wm.match("testmonkey"));
		Assert.assertTrue(wm.match("test1monkey"));
		Assert.assertTrue(wm.match("test234monkey"));
		Assert.assertFalse(wm.match("1testmonkey"));
		Assert.assertFalse(wm.match("1test1monkey"));
		Assert.assertFalse(wm.match("1test234monkey"));
		Assert.assertFalse(wm.match("1testmonkey1"));
		Assert.assertFalse(wm.match("1test1monkey1"));
		Assert.assertFalse(wm.match("1test234monkey1"));

		wm = new WildcardMatcher("test#monkey");
		Assert.assertEquals(3, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("test", part.str);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(1, part.num);
		part = wm.parts.get(2);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("monkey", part.str);

		Assert.assertFalse(wm.match("testmonkey"));
		Assert.assertTrue(wm.match("test1monkey"));
		Assert.assertFalse(wm.match("test12monkey"));
		Assert.assertFalse(wm.match("1testmonkey"));
		Assert.assertFalse(wm.match("1test1monkey"));
		Assert.assertFalse(wm.match("1test234monkey"));
		Assert.assertFalse(wm.match("1testmonkey1"));
		Assert.assertFalse(wm.match("1test1monkey1"));
		Assert.assertFalse(wm.match("1test234monkey1"));

		wm = new WildcardMatcher("test##monkey");
		Assert.assertEquals(3, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("test", part.str);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_HASH, part.type);
		Assert.assertEquals(2, part.num);
		part = wm.parts.get(2);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("monkey", part.str);

		Assert.assertFalse(wm.match("testmonkey"));
		Assert.assertFalse(wm.match("test1monkey"));
		Assert.assertTrue(wm.match("test12monkey"));
		Assert.assertFalse(wm.match("test123monkey"));
		Assert.assertFalse(wm.match("1testmonkey"));
		Assert.assertFalse(wm.match("1test1monkey"));
		Assert.assertFalse(wm.match("1test234monkey"));
		Assert.assertFalse(wm.match("1testmonkey1"));
		Assert.assertFalse(wm.match("1test1monkey1"));
		Assert.assertFalse(wm.match("1test234monkey1"));

		wm = new WildcardMatcher("*1*2*3*");
		Assert.assertEquals(7, wm.parts.size());
		part = wm.parts.get(0);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(1);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("1", part.str);
		part = wm.parts.get(2);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(3);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("2", part.str);
		part = wm.parts.get(4);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);
		part = wm.parts.get(5);
		Assert.assertEquals(WildcardMatcher.WCT_STRING, part.type);
		Assert.assertEquals("3", part.str);
		part = wm.parts.get(6);
		Assert.assertEquals(WildcardMatcher.WCT_ASTERIX, part.type);

		Assert.assertTrue(wm.match("112123"));
	}

}
