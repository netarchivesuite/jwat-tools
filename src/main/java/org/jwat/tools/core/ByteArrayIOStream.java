package org.jwat.tools.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public class ByteArrayIOStream {

	private Semaphore lock = new Semaphore(1);

	private byte[] bytes = new byte[10*1024*1024];

    private ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

    private int limit = 0;

    public OutputStream getOutputStream() {
    	if (!lock.tryAcquire()) {
    		throw new IllegalStateException();
    	}
		byteBuffer.clear();
		limit = 0;
    	return new OutputStreamImpl(this);
    }

    public int getLimit() {
    	return limit;
    }

    public ByteBuffer getBuffer() {
    	ByteBuffer buffer = ByteBuffer.wrap(bytes);
    	buffer.position(0);
    	buffer.limit(limit);
    	return buffer;
    }

    public InputStream getInputStream() {
    	if (!lock.tryAcquire()) {
    		throw new IllegalStateException();
    	}
    	byteBuffer.clear();
    	byteBuffer.limit(limit);
    	return new InputStreamImpl(this);
    }

    /*
     * OutputStream.
     */

    public static class OutputStreamImpl extends OutputStream {

		protected ByteArrayIOStream baios;
		protected ByteBuffer byteBuffer;

    	protected OutputStreamImpl(ByteArrayIOStream baios) {
    		this.baios = baios;
    		this.byteBuffer = baios.byteBuffer;
    	}

    	@Override
    	public void close() {
    		if (baios != null) {
    			baios.limit = baios.byteBuffer.position();
    			baios.lock.release();
    			baios = null;
    			byteBuffer = null;
    		}
    	}

    	@Override
		public void flush() throws IOException {
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			byteBuffer.put(b, off, len);
		}

		@Override
		public void write(byte[] b) throws IOException {
			byteBuffer.put(b);
		}

    	@Override
		public void write(int b) throws IOException {
    		byteBuffer.put((byte)b);
		}

    }

    /*
     * InputStream.
     */

    public static class InputStreamImpl extends InputStream {

    	protected ByteArrayIOStream baios;
		protected ByteBuffer byteBuffer;

    	protected InputStreamImpl(ByteArrayIOStream baios) {
    		this.baios = baios;
    		this.byteBuffer = baios.byteBuffer;
    	}

    	@Override
    	public void close() {
    		if (baios != null) {
    			baios.lock.release();
    			baios = null;
    			byteBuffer = null;
    		}
    	}

		@Override
		public int available() throws IOException {
			return byteBuffer.limit() - byteBuffer.position();
		}

		@Override
		public boolean markSupported() {
			return false;
		}

		@Override
		public synchronized void mark(int readlimit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public synchronized void reset() throws IOException {
			throw new UnsupportedOperationException();
		}

    	@Override
		public int read() throws IOException {
    		if (byteBuffer.remaining() > 0) {
    			return byteBuffer.get();
    		} else {
    			return -1;
    		}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int remaining = byteBuffer.remaining();
			if (len > remaining) {
				len = remaining;
			}
			if (len > 0) {
				byteBuffer.get(b, off, len);
				return len;
			} else {
				return -1;
			}
		}

		@Override
		public int read(byte[] b) throws IOException {
			int len = b.length;
			int remaining = byteBuffer.remaining();
			if (len > remaining) {
				len = remaining;
			}
			if (len > 0) {
				byteBuffer.get(b, 0, len);
				return len;
			} else {
				return -1;
			}
		}

		@Override
		public long skip(long n) throws IOException {
			int remaining = byteBuffer.remaining();
			if (n > remaining) {
				n = remaining;
			}
			if (n > 0) {
				byteBuffer.position(byteBuffer.position() + (int)n);
			}
			return n;
		}

    }

}
