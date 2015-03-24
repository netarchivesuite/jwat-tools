package org.jwat.tools.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestArrayUtils {

	@Test
	public void test_arrayutils() {
		ArrayUtils arrayUtils = new ArrayUtils();
		Assert.assertNotNull(arrayUtils);

		byte[] arr;
		byte[] arr2;

		/*
		 * skipSpaces().
		 */

		arr = "                ".getBytes();

		Assert.assertEquals(16, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 0));
		Assert.assertEquals(16, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 8));
		Assert.assertEquals(16, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 16));
		Assert.assertEquals(17, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 17));

		arr = "0123456789abcdef".getBytes();
		Assert.assertEquals(0, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 0));
		Assert.assertEquals(8, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 8));
		Assert.assertEquals(16, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 16));
		Assert.assertEquals(17, ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE, arr, 17));

		Assert.assertEquals(9, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 0));
		Assert.assertEquals(9, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 9));
		Assert.assertEquals(10, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 10));
		Assert.assertEquals(13, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 11));
		Assert.assertEquals(13, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 13));
		Assert.assertEquals(32, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 14));
		Assert.assertEquals(32, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 32));
		Assert.assertEquals(256, ArrayUtils.skip(ArrayUtils.SKIP_NONWHITESPACE, ArrayUtils.CASE_SENSITIVE, 33));

		/*
		 * equalsAt().
		 */

		arr = " <HTML> ".getBytes();
		arr2 = " <html> ".getBytes();

		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr, 0));
		Assert.assertTrue(ArrayUtils.equalsAt("<HTML>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr, 2));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr, 5));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr, 10));

		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr, 0));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr, 2));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr, 5));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr, 10));

		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr2, 0));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr2, 2));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr2, 5));
		Assert.assertFalse(ArrayUtils.equalsAt("<HTML>".getBytes(), arr2, 10));

		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr2, 0));
		Assert.assertTrue(ArrayUtils.equalsAt("<html>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr2, 2));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr2, 5));
		Assert.assertFalse(ArrayUtils.equalsAt("<html>".getBytes(), arr2, 10));

		Assert.assertFalse(ArrayUtils.equalsAt("<HEAD>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<head>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<HEAD>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAt("<head>".getBytes(), arr2, 1));

		Assert.assertFalse(ArrayUtils.equalsAt("<HEAD>".getBytes(), ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertFalse(ArrayUtils.equalsAt("<head>".getBytes(), ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertFalse(ArrayUtils.equalsAt(new byte[] {(byte)192, (byte)193}, ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertFalse(ArrayUtils.equalsAt(new byte[] {(byte)192, (byte)193}, ArrayUtils.CASE_INSENSITIVE, 192));

		/*
		 * equalsAtIgnoreCase().
		 */

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr, 0));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr, 2));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr, 5));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr, 10));

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr, 0));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr, 2));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr, 5));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr, 10));

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr2, 0));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr2, 2));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr2, 5));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HTML>".getBytes(), arr2, 10));

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr2, 0));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr2, 2));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr2, 5));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<html>".getBytes(), arr2, 10));

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HEAD>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<head>".getBytes(), arr, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HEAD>".getBytes(), arr2, 1));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<head>".getBytes(), arr2, 1));

		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<HEAD>".getBytes(), ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertFalse(ArrayUtils.equalsAtIgnoreCase("<head>".getBytes(), ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase(new byte[] {(byte)192, (byte)193}, ArrayUtils.CASE_INSENSITIVE, 192));
		Assert.assertTrue(ArrayUtils.equalsAtIgnoreCase(new byte[] {(byte)192, (byte)193}, ArrayUtils.CASE_INSENSITIVE, 192));
	}

}
