package org.jwat.tools.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.PayloadWithHeaderAbstract;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.warc.WarcRecord;

public class ManagedPayload {

	private static final int DEFAULT_COPY_BUFFER_SIZE = 8192;

	private static final int DEFAULT_IN_MEMORY_BUFFER_SIZE = 10*1024*1024;

	protected static Semaphore queueLock = new Semaphore(1);

	protected static ConcurrentLinkedQueue<ManagedPayload> managedPayloadQueue = new ConcurrentLinkedQueue<ManagedPayload>();

	public static ManagedPayload checkout() {
		ManagedPayload managedPayload = null;
		queueLock.acquireUninterruptibly();
		managedPayload = managedPayloadQueue.poll();
		if (managedPayload == null) {
			managedPayload = new ManagedPayload(DEFAULT_COPY_BUFFER_SIZE, DEFAULT_IN_MEMORY_BUFFER_SIZE);
		}
		if (!managedPayload.lock.tryAcquire()) {
			throw new IllegalStateException();
		}
		queueLock.release();
		return managedPayload;
	}

	public void checkin() {
		queueLock.acquireUninterruptibly();
		lock.release();
		managedPayloadQueue.add(this);
		queueLock.release();
	}

	protected Semaphore lock = new Semaphore(1);

	protected MessageDigest blockDigestObj;

	protected MessageDigest payloadDigestObj;

    public byte[] blockDigestBytes;

    public byte[] payloadDigestBytes;

    public int type = 0;

    protected byte[] copyBuf;

    protected File tmpfile;

    protected ByteArrayIOStream baios;

    protected RandomAccessFile tmpfile_raf; 

	public long payloadLength;

	public HttpHeader httpHeader;

	public byte[] httpHeaderBytes;

	public long httpHeaderLength;

    protected ManagedPayload(int copyBufferSize, int inMemorybufferSize) {
        try {
            blockDigestObj = MessageDigest.getInstance("SHA1");
            payloadDigestObj = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        copyBuf = new byte[copyBufferSize];
        baios = new ByteArrayIOStream(inMemorybufferSize);
    }

    // THL
    private void closeRaf() {
		if (tmpfile_raf != null) {
			try {
				tmpfile_raf.close();
				tmpfile_raf = null;
			} catch (IOException e) {
			}
		}
    }

    public void close() {
    	closeRaf();
		if (tmpfile != null && tmpfile.exists()) {
			tmpfile.delete();
			tmpfile = null;
		}			
    }

	public void manageVersionBlock(ArcRecordBase arcRecord, boolean bDigest) throws IOException {
		blockDigestObj.reset();
		blockDigestBytes = null;
		httpHeader = null;
		httpHeaderBytes = null;
		httpHeaderLength = 0;
    	payloadLength = 0;

    	OutputStream out;
    	type = 0;

    	Payload payload = arcRecord.getPayload();
		InputStream payload_in = null;
    	int read;
		if ((arcRecord.header.headerBytes.length + arcRecord.getArchiveLength()) < 10*1024*1024L) {
			type = 1;
			out = baios.getOutputStream();
			if (bDigest) {
	            blockDigestObj.update(arcRecord.header.headerBytes);
			}
            out.write(arcRecord.header.headerBytes);
    		if (payload != null) {
    			payload_in = payload.getInputStreamComplete();
    			while((read = payload_in.read(copyBuf)) != -1) {
    				if (bDigest) {
                        blockDigestObj.update(copyBuf, 0, read);
    				}
                    out.write(copyBuf, 0, read);
    			}
    		}
            out.close();
            payloadLength = (long)baios.getLimit();
		} else {
			type = 2;
			if (tmpfile == null) {
				tmpfile = File.createTempFile("JWAT-", "-ARC2WARC");
		        tmpfile_raf = new RandomAccessFile(tmpfile, "rw");
			}
	        tmpfile_raf.seek(0L);
	        tmpfile_raf.setLength(0L);
			if (bDigest) {
	            blockDigestObj.update(arcRecord.header.headerBytes);
			}
            tmpfile_raf.write(arcRecord.header.headerBytes);
    		if (payload != null) {
    			payload_in = payload.getInputStreamComplete();
    			while((read = payload_in.read(copyBuf)) != -1) {
    				if (bDigest) {
                        blockDigestObj.update(copyBuf, 0, read);
    				}
                    tmpfile_raf.write(copyBuf, 0, read);
    			}
    		}
	        tmpfile_raf.seek(0L);
	        payloadLength = tmpfile_raf.length();
	        closeRaf();
	    }
		if (bDigest) {
	        blockDigestBytes = blockDigestObj.digest();
		}
		if (payload_in != null) {
			payload_in.close();
		}
		if (payload != null) {
			payload.close();
		}
    }

	public void manageArcRecord(ArcRecordBase arcRecord, boolean bDigest) throws IOException {
		manageRecord(arcRecord.getPayload(), bDigest);
	}

	public void manageWarcRecord(WarcRecord warcRecord, boolean bDigest) throws IOException {
    	manageRecord(warcRecord.getPayload(), bDigest);
	}

	public void manageRecord(Payload payload, boolean bDigest) throws IOException {
    	blockDigestObj.reset();
		payloadDigestObj.reset();
        blockDigestBytes = null;
        payloadDigestBytes = null;
		httpHeader = null;
		httpHeaderBytes = null;
		httpHeaderLength = 0;
    	payloadLength = 0;

    	OutputStream out;
    	type = 0;

		PayloadWithHeaderAbstract payloadHeaderWrapped;
		InputStream payload_in = null;
        int read;
		if (payload != null) {
			payloadHeaderWrapped = payload.getPayloadHeaderWrapped();
			if (payloadHeaderWrapped instanceof HttpHeader) {
				httpHeader = (HttpHeader)payloadHeaderWrapped;
			}
			if (httpHeader != null && httpHeader.isValid()) {
				httpHeaderBytes = httpHeader.getHeader();
				if (bDigest) {
	                blockDigestObj.update(httpHeaderBytes);
				}
				httpHeaderLength = httpHeaderBytes.length;
			}
			if (payload.getRemaining() < 10*1024*1024L) {
				type = 1;
				out = baios.getOutputStream();
    			payload_in = payload.getInputStream();
    			while((read = payload_in.read(copyBuf)) != -1) {
    				if (bDigest) {
                        blockDigestObj.update(copyBuf, 0, read);
                        payloadDigestObj.update(copyBuf, 0, read);
    				}
                    out.write(copyBuf, 0, read);
    			}
	            out.close();
	            payloadLength = (long)baios.getLimit();
			} else {
				type = 2;
				if (tmpfile == null) {
					tmpfile = File.createTempFile("JWAT-", "-ARC2WARC");
			        tmpfile_raf = new RandomAccessFile(tmpfile, "rw");
				}
		        tmpfile_raf.seek(0L);			// NPE if raf is closed but tmpfile exists.
		        tmpfile_raf.setLength(0L);
    			payload_in = payload.getInputStream();
    			while((read = payload_in.read(copyBuf)) != -1) {
    				if (bDigest) {
                        blockDigestObj.update(copyBuf, 0, read);
                        payloadDigestObj.update(copyBuf, 0, read);
    				}
                    tmpfile_raf.write(copyBuf, 0, read);
    			}
		        tmpfile_raf.seek(0L);
		        payloadLength = tmpfile_raf.length();
		        closeRaf();
			}
		}
		if (bDigest) {
	        blockDigestBytes = blockDigestObj.digest();
	        payloadDigestBytes = payloadDigestObj.digest();
		}
		if (payload_in != null) {
			payload_in.close();
		}
		if (payload != null) {
			payload.close();
		}
	}

	public void managedHttp(byte[] bytes, boolean bDigest) throws IOException {
    	blockDigestObj.reset();
		payloadDigestObj.reset();
        blockDigestBytes = null;
        payloadDigestBytes = null;

		httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, new ByteCountingPushBackInputStream(new ByteArrayInputStream(bytes), 16384), bytes.length, null);
		if (httpHeader == null || !httpHeader.isValid()) {
			throw new IllegalStateException();
		}
		httpHeaderBytes = httpHeader.getHeader();
		if (bDigest) {
            blockDigestObj.update(httpHeaderBytes);
		}
		httpHeaderLength = httpHeaderBytes.length;
	}

	public void managePayloadInputStream(InputStream in, long len, boolean bDigest) throws IOException {
    	payloadLength = 0;

    	OutputStream out;
    	type = 0;

        int read;
		if (len < 10*1024*1024L) {
			type = 1;
			out = baios.getOutputStream();
			while((read = in.read(copyBuf)) != -1) {
				if (bDigest) {
                    blockDigestObj.update(copyBuf, 0, read);
                    payloadDigestObj.update(copyBuf, 0, read);
				}
                out.write(copyBuf, 0, read);
			}
            out.close();
            payloadLength = (long)baios.getLimit();
		} else {
			type = 2;
			if (tmpfile == null) {
				tmpfile = File.createTempFile("JWAT-", "-ARC2WARC");
		        tmpfile_raf = new RandomAccessFile(tmpfile, "rw");
			}
	        tmpfile_raf.seek(0L);
	        tmpfile_raf.setLength(0L);
			while((read = in.read(copyBuf)) != -1) {
				if (bDigest) {
                    blockDigestObj.update(copyBuf, 0, read);
                    payloadDigestObj.update(copyBuf, 0, read);
				}
                tmpfile_raf.write(copyBuf, 0, read);
			}
	        tmpfile_raf.seek(0L);
	        payloadLength = tmpfile_raf.length();
	        closeRaf();
		}
		if (bDigest) {
	        blockDigestBytes = blockDigestObj.digest();
	        payloadDigestBytes = payloadDigestObj.digest();
		}
		in.close();
		if (payloadLength != len) {
			throw new IllegalStateException();
		}
	}

	public ByteBuffer getBuffer() {
		return baios.getBuffer();
	}

	public File getFile() {
		return tmpfile;
	}

	public InputStream getHttpHeaderStream() {
		if (httpHeaderBytes != null) {
			return new ByteArrayInputStream(httpHeaderBytes);
		} else {
			return null;
		}
	}

	public InputStream getPayloadStream() throws IOException {
		switch (type) {
		case 1:
		    return baios.getInputStream();
		case 2:
			if (tmpfile_raf == null) {
				tmpfile_raf = new RandomAccessFile(tmpfile, "rw");
			}
	        tmpfile_raf.seek(0L);
		    return new RandomAccessFileInputStream(tmpfile_raf);
		default:
			return null;
		}
	}

}
