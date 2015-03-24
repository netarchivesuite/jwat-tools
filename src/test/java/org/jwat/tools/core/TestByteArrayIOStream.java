package org.jwat.tools.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.tools.core.ByteArrayIOStream.InputStreamImpl;
import org.jwat.tools.core.ByteArrayIOStream.OutputStreamImpl;

@RunWith(JUnit4.class)
public class TestByteArrayIOStream {

	private SecureRandom random = new SecureRandom();

	private byte[] tmpBuf = new byte[16];

	private byte[] zeroBuf = new byte[0];

	@Test
	public void test_bytearrayiostream_default() {
		ByteArrayIOStream baios = new ByteArrayIOStream();
		Assert.assertNotNull(baios);
		Assert.assertNotNull(baios.lock);
		Assert.assertEquals(ByteArrayIOStream.DEFAULT_BUFFER_SIZE, baios.bytes.length);
		Assert.assertEquals(baios.bytes, baios.byteBuffer.array());
		//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
		Assert.assertEquals(0, baios.limit);
		Assert.assertEquals(baios.limit, baios.getLimit());
		Assert.assertEquals(0, baios.byteBuffer.position());
		Assert.assertEquals(ByteArrayIOStream.DEFAULT_BUFFER_SIZE, baios.byteBuffer.limit());
		/*
		 * release().
		 */
		baios.release();
		Assert.assertNull(baios.lock);
		Assert.assertNull(baios.bytes);
		Assert.assertNull(baios.byteBuffer);
		Assert.assertEquals(0, baios.limit);
		//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
		Assert.assertEquals(baios.limit, baios.getLimit());
	}

	@Test
	public void test_bytearrayiostream_input() {
		byte[] tmpBuf;
		try {
			ByteArrayIOStream baios = new ByteArrayIOStream(8192);
			Assert.assertNotNull(baios);
			Assert.assertNotNull(baios.lock);
			Assert.assertEquals(8192, baios.bytes.length);
			Assert.assertEquals(baios.bytes, baios.byteBuffer.array());
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(0, baios.limit);
			Assert.assertEquals(baios.limit, baios.getLimit());
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			/*
			 * getByteBuffer().
			 */
			ByteBuffer byteBuffer = baios.getBuffer();
			Assert.assertEquals(0, byteBuffer.position());
			Assert.assertEquals(0, byteBuffer.limit());
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			/*
			 * getInputStream().
			 */
			InputStreamImpl in = (InputStreamImpl)baios.getInputStream();
	    	Assert.assertEquals(baios, in.baios);
	    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(0, baios.byteBuffer.limit());
			try {
				baios.getInputStream();
				Assert.fail("Exception expected!");
			} catch (IllegalStateException e) {
			}
			Assert.assertEquals(0, in.available());
			Assert.assertFalse(in.markSupported());
			try {
				in.mark(42);
				Assert.fail("Exception expected!");
			} catch (UnsupportedOperationException e) {
			}
			try {
				in.reset();
				Assert.fail("Exception expected!");
			} catch (UnsupportedOperationException e) {
			}
			Assert.assertEquals(-1, in.read());
			tmpBuf = new byte[1];
			Assert.assertEquals(-1, in.read(tmpBuf));
			Assert.assertEquals(-1, in.read(tmpBuf, 0, tmpBuf.length));
			Assert.assertEquals(0, in.skip(1));
	    	Assert.assertEquals(baios, in.baios);
	    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
	    	in.close();
	    	Assert.assertNull(in.baios);
	    	Assert.assertNull(in.byteBuffer);
	    	in.close();
	    	Assert.assertNull(in.baios);
	    	Assert.assertNull(in.byteBuffer);
			/*
			 * release().
			 */
			baios.release();
			Assert.assertNull(baios.lock);
			Assert.assertNull(baios.bytes);
			Assert.assertNull(baios.byteBuffer);
			Assert.assertEquals(0, baios.limit);
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(baios.limit, baios.getLimit());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

	@Test
	public void test_bytearrayiostream_output_none() {
		byte[] tmpBuf;
		try {
			ByteArrayIOStream baios = new ByteArrayIOStream(8192);
			Assert.assertNotNull(baios);
			Assert.assertNotNull(baios.lock);
			Assert.assertEquals(8192, baios.bytes.length);
			Assert.assertEquals(baios.bytes, baios.byteBuffer.array());
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(0, baios.limit);
			Assert.assertEquals(baios.limit, baios.getLimit());
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			/*
			 * getOutputStream().
			 */
			OutputStreamImpl out = (OutputStreamImpl)baios.getOutputStream();
	    	Assert.assertEquals(baios, out.baios);
	    	Assert.assertEquals(baios.byteBuffer, out.byteBuffer);
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			try {
				baios.getOutputStream();
				Assert.fail("Exception expected!");
			} catch (IllegalStateException e) {
			}
	    	Assert.assertEquals(baios, out.baios);
	    	Assert.assertEquals(baios.byteBuffer, out.byteBuffer);
			out.flush();
			out.close();
	    	Assert.assertNull(out.baios);
	    	Assert.assertNull(out.byteBuffer);
			out.close();
	    	Assert.assertNull(out.baios);
	    	Assert.assertNull(out.byteBuffer);
			/*
			 * getByteBuffer().
			 */
			ByteBuffer byteBuffer = baios.getBuffer();
			Assert.assertEquals(0, byteBuffer.position());
			Assert.assertEquals(0, byteBuffer.limit());
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			/*
			 * getInputStream().
			 */
			InputStreamImpl in = (InputStreamImpl)baios.getInputStream();
	    	Assert.assertEquals(baios, in.baios);
	    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(0, baios.byteBuffer.limit());
			try {
				baios.getInputStream();
				Assert.fail("Exception expected!");
			} catch (IllegalStateException e) {
			}
			Assert.assertEquals(0, in.available());
			Assert.assertFalse(in.markSupported());
			try {
				in.mark(42);
				Assert.fail("Exception expected!");
			} catch (UnsupportedOperationException e) {
			}
			try {
				in.reset();
				Assert.fail("Exception expected!");
			} catch (UnsupportedOperationException e) {
			}
			Assert.assertEquals(-1, in.read());
			tmpBuf = new byte[1];
			Assert.assertEquals(-1, in.read(tmpBuf));
			Assert.assertEquals(-1, in.read(tmpBuf, 0, tmpBuf.length));
			Assert.assertEquals(0, in.skip(1));
	    	Assert.assertEquals(baios, in.baios);
	    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
	    	in.close();
	    	Assert.assertNull(in.baios);
	    	Assert.assertNull(in.byteBuffer);
	    	in.close();
	    	Assert.assertNull(in.baios);
	    	Assert.assertNull(in.byteBuffer);
			/*
			 * release().
			 */
			baios.release();
			Assert.assertNull(baios.lock);
			Assert.assertNull(baios.bytes);
			Assert.assertNull(baios.byteBuffer);
			Assert.assertEquals(0, baios.limit);
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(baios.limit, baios.getLimit());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

	@Test
	public void test_bytearrayiostream_output_input() {
		byte[] srcBuf;
		byte[] dstBuf;
		byte[] dstBuf2;
		try {
			ByteArrayIOStream baios = new ByteArrayIOStream(8192);
			Assert.assertNotNull(baios);
			Assert.assertNotNull(baios.lock);
			Assert.assertEquals(8192, baios.bytes.length);
			Assert.assertEquals(baios.bytes, baios.byteBuffer.array());
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(0, baios.limit);
			Assert.assertEquals(baios.limit, baios.getLimit());
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());

			srcBuf = new byte[8192];
			dstBuf = new byte[srcBuf.length];
			dstBuf2 = new byte[srcBuf.length];
			random.nextBytes(srcBuf);

			/*
			 * getOutputStream().
			 */
			OutputStreamImpl out = (OutputStreamImpl)baios.getOutputStream();
	    	Assert.assertEquals(baios, out.baios);
	    	Assert.assertEquals(baios.byteBuffer, out.byteBuffer);
			Assert.assertEquals(0, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			try {
				baios.getOutputStream();
				Assert.fail("Exception expected!");
			} catch (IllegalStateException e) {
			}
	    	Assert.assertEquals(baios, out.baios);
	    	Assert.assertEquals(baios.byteBuffer, out.byteBuffer);
	    	int pos = 0;
	    	int limit = srcBuf.length;
	    	int mod = 0;
	    	int b;
	    	int len;
	    	while (pos < limit) {
	    		switch (mod) {
	    		case 0:
	    			b = srcBuf[pos++];
	    			out.write(b);
	    			break;
	    		case 1:
	    			if (limit - pos > tmpBuf.length) {
	    				System.arraycopy(srcBuf, pos, tmpBuf, 0, tmpBuf.length);
	    				out.write(tmpBuf);
	    				pos += tmpBuf.length;
	    			}
	    			break;
	    		case 2:
	    			len = random.nextInt(16);
	    			if (len > (limit - pos)) {
	    				len = limit - pos;
	    			}
	    			out.write(srcBuf, pos, len);
	    			pos += len;
	    			break;
	    		}
	    		mod = (mod + 1) % 3;
	    	}
	    	out.flush();
			out.close();
	    	Assert.assertNull(out.baios);
	    	Assert.assertNull(out.byteBuffer);
			out.close();
	    	Assert.assertNull(out.baios);
	    	Assert.assertNull(out.byteBuffer);
			/*
			 * getByteBuffer().
			 */
			ByteBuffer byteBuffer = baios.getBuffer();
			Assert.assertEquals(0, byteBuffer.position());
			Assert.assertEquals(srcBuf.length, byteBuffer.limit());
			Assert.assertEquals(srcBuf.length, baios.byteBuffer.position());
			Assert.assertEquals(8192, baios.byteBuffer.limit());
			assertInputStream_read(baios, srcBuf, dstBuf);
			assertInputStream_read_skip(baios, srcBuf, dstBuf);
			assertInputStream_read(baios, srcBuf, dstBuf2);
			assertInputStream_read_skip(baios, srcBuf, dstBuf2);
			/*
			 * release().
			 */
			baios.release();
			Assert.assertNull(baios.lock);
			Assert.assertNull(baios.bytes);
			Assert.assertNull(baios.byteBuffer);
			Assert.assertEquals(0, baios.limit);
			//Assert.assertEquals(baios.byteBuffer, baios.getBuffer());
			Assert.assertEquals(baios.limit, baios.getLimit());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

	public void assertInputStream_read(ByteArrayIOStream baios, byte[] srcBuf, byte[] dstBuf) throws IOException {
		/*
		 * getInputStream() - first pass.
		 */
		InputStreamImpl in = (InputStreamImpl)baios.getInputStream();
    	Assert.assertEquals(baios, in.baios);
    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
		Assert.assertEquals(0, baios.byteBuffer.position());
		Assert.assertEquals(srcBuf.length, baios.byteBuffer.limit());
		try {
			baios.getInputStream();
			Assert.fail("Exception expected!");
		} catch (IllegalStateException e) {
		}
		Assert.assertEquals(in.byteBuffer.remaining(), in.available());
		Assert.assertFalse(in.markSupported());
		try {
			in.mark(42);
			Assert.fail("Exception expected!");
		} catch (UnsupportedOperationException e) {
		}
		try {
			in.reset();
			Assert.fail("Exception expected!");
		} catch (UnsupportedOperationException e) {
		}
    	int pos = 0;
    	int limit = srcBuf.length;
    	int mod = 0;
    	int b;
    	int len;
    	while (pos < limit) {
    		switch (mod) {
    		case 0:
    			b = in.read();
    			dstBuf[pos++] = (byte)(b & 255);
    			break;
    		case 1:
    			len = in.read(tmpBuf);
    			System.arraycopy(tmpBuf, 0, dstBuf, pos, len);
    			pos += len;
    			break;
    		case 2:
    			len = random.nextInt(16);
    			if (len > (limit - pos)) {
    				len = limit - pos;
    			}
    			len = in.read(dstBuf, pos, len);
    			pos += len;
    			break;
    		case 3:
    			len = in.read(zeroBuf);
    			pos += len;
    			break;
    		}
    		mod = (mod + 1) % 4;
    	}
    	Assert.assertArrayEquals(srcBuf, dstBuf);
		Assert.assertEquals(-1, in.read());
		tmpBuf = new byte[1];
		Assert.assertEquals(-1, in.read(tmpBuf));
		Assert.assertEquals(-1, in.read(tmpBuf, 0, tmpBuf.length));
		Assert.assertEquals(0, in.skip(1));
    	Assert.assertEquals(baios, in.baios);
    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
    	in.close();
    	Assert.assertNull(in.baios);
    	Assert.assertNull(in.byteBuffer);
    	in.close();
    	Assert.assertNull(in.baios);
    	Assert.assertNull(in.byteBuffer);
	}

	public void assertInputStream_read_skip(ByteArrayIOStream baios, byte[] srcBuf, byte[] dstBuf) throws IOException {
		/*
		 * getInputStream() - first pass.
		 */
		InputStreamImpl in = (InputStreamImpl)baios.getInputStream();
    	Assert.assertEquals(baios, in.baios);
    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
		Assert.assertEquals(0, baios.byteBuffer.position());
		Assert.assertEquals(srcBuf.length, baios.byteBuffer.limit());
		try {
			baios.getInputStream();
			Assert.fail("Exception expected!");
		} catch (IllegalStateException e) {
		}
		Assert.assertEquals(in.byteBuffer.remaining(), in.available());
		Assert.assertFalse(in.markSupported());
		try {
			in.mark(42);
			Assert.fail("Exception expected!");
		} catch (UnsupportedOperationException e) {
		}
		try {
			in.reset();
			Assert.fail("Exception expected!");
		} catch (UnsupportedOperationException e) {
		}
    	int pos = 0;
    	int limit = srcBuf.length;
    	int mod = 0;
    	int b;
    	int len;
    	while (pos < limit) {
    		switch (mod) {
    		case 0:
    			b = in.read();
    			dstBuf[pos++] = (byte)(b & 255);
    			break;
    		case 1:
    			len = in.read(tmpBuf);
    			System.arraycopy(tmpBuf, 0, dstBuf, pos, len);
    			pos += len;
    			break;
    		case 2:
    			len = random.nextInt(16);
    			if (len > (limit - pos)) {
    				len = limit - pos;
    			}
    			len = in.read(dstBuf, pos, len);
    			pos += len;
    			break;
    		case 3:
    			len = in.read(zeroBuf);
    			pos += len;
    			break;
    		case 4:
    			len = random.nextInt(16);
    			len = (int)in.skip(len);
    			pos += len;
    			break;
    		}
    		mod = (mod + 1) % 5;
    	}
    	Assert.assertArrayEquals(srcBuf, dstBuf);
		Assert.assertEquals(-1, in.read());
		tmpBuf = new byte[1];
		Assert.assertEquals(-1, in.read(tmpBuf));
		Assert.assertEquals(-1, in.read(tmpBuf, 0, tmpBuf.length));
		Assert.assertEquals(0, in.skip(1));
    	Assert.assertEquals(baios, in.baios);
    	Assert.assertEquals(baios.byteBuffer, in.byteBuffer);
    	in.close();
    	Assert.assertNull(in.baios);
    	Assert.assertNull(in.byteBuffer);
    	in.close();
    	Assert.assertNull(in.baios);
    	Assert.assertNull(in.byteBuffer);
	}

}
