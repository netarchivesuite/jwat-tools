package org.jwat.tools.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.jwat.arc.ArcRecordBase;
import org.jwat.arc.ArcWriter;
import org.jwat.arc.ArcWriterFactory;
import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class Cloner {

	private static Cloner cloner;

	public static synchronized Cloner getCloner() {
		if (cloner == null) {
			cloner = new Cloner();
		}
		return cloner;
	}

	private Cloner() {
	}

	private RandomAccessFile arcRaf;
	private RandomAccessFileOutputStream arcRafOut;
	private ArcWriter arcWriter;

	private RandomAccessFile warcRaf;
	private RandomAccessFileOutputStream warcRafOut;
	private WarcWriter warcWriter;

	public synchronized void cloneArcRecord(ArcRecordBase record, ManagedPayload managedPayload) throws IOException {
		if (arcWriter == null) {
			arcRaf = new RandomAccessFile("erroneous.arc", "rw");
			arcRaf.seek(0);
			arcRaf.setLength(0);
			arcRafOut = new RandomAccessFileOutputStream(arcRaf);
			arcWriter = ArcWriterFactory.getWriter(arcRafOut, 8192, false);
		}
		arcWriter.writeHeader(record);
		InputStream httpHeaderStream = managedPayload.getHttpHeaderStream();
		if (httpHeaderStream != null) {
			arcWriter.streamPayload(httpHeaderStream);
			httpHeaderStream.close();
			httpHeaderStream = null;
		}
		InputStream payloadStream = managedPayload.getPayloadStream();
		if (payloadStream != null) {
			arcWriter.streamPayload(payloadStream);
			payloadStream.close();
			payloadStream = null;
		}
		arcWriter.closeRecord();
	}

	public synchronized void cloneWarcRecord(WarcRecord record, ManagedPayload managedPayload) throws IOException {
		if (warcWriter == null) {
			warcRaf = new RandomAccessFile("erroneous.warc", "rw");
			warcRaf.seek(0);
			warcRaf.setLength(0);
			warcRafOut = new RandomAccessFileOutputStream(warcRaf);
			warcWriter = WarcWriterFactory.getWriter(warcRafOut, 8192, false);
		}
		warcWriter.writeHeader(record);
		InputStream httpHeaderStream = managedPayload.getHttpHeaderStream();
		if (httpHeaderStream != null) {
			warcWriter.streamPayload(httpHeaderStream);
			httpHeaderStream.close();
			httpHeaderStream = null;
		}
		InputStream payloadStream = managedPayload.getPayloadStream();
		if (payloadStream != null) {
			warcWriter.streamPayload(payloadStream);
			payloadStream.close();
			payloadStream = null;
		}
		warcWriter.closeRecord();
	}

	public void close() throws IOException {
		if (arcWriter != null) {
			arcWriter.close();
			arcWriter = null;
		}
		if (warcWriter != null) {
			warcWriter.close();
			warcWriter = null;
		}
		if (arcRafOut != null) {
			arcRafOut.close();
			arcRafOut = null;
		}
		if (warcRafOut != null) {
			warcRafOut.close();
			warcRafOut = null;
		}
		if (arcRaf != null) {
			arcRaf.close();
			arcRaf = null;
		}
		if (warcRaf != null) {
			warcRaf.close();
			warcRaf = null;
		}
	}

}
