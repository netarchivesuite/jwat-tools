package org.jwat.tools.tasks.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ArchiveParser;
import org.jwat.archive.ArchiveParserCallback;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.tools.tasks.ResultItemThrowable;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcRecord;

public class ExtractFile implements ArchiveParserCallback {

	protected ExtractOptions options;

    protected ExtractResult result;

	protected byte[] tmpBuf = new byte[8192];

	public ExtractFile() {
	}

	public ExtractResult processFile(File srcFile, ExtractOptions options) {
		this.options = options;
		result = new ExtractResult();
		result.srcFile = srcFile;
		result.fileName = srcFile.getName();
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = options.bValidateDigest;
		archiveParser.bPayloadDigestEnabled = options.bValidateDigest;
	    archiveParser.recordHeaderMaxSize = options.recordHeaderMaxSize;
	    archiveParser.payloadHeaderMaxSize = options.payloadHeaderMaxSize;
		result.consumed = archiveParser.parse(srcFile, this);
		return result;
	}

	@Override
	public void apcFileId(File file, int fileId) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException {
		if (options.targetUri == null || options.targetUri.equalsIgnoreCase(arcRecord.header.urlStr)) {
			Payload payload = arcRecord.getPayload();
			HttpHeader httpHeader = null;
			InputStream payloadStream = null;
			if (payload != null) {
				httpHeader = arcRecord.getHttpHeader();
				if (httpHeader != null ) {
					payloadStream = httpHeader.getPayloadInputStream();
				} else {
					payloadStream = payload.getInputStreamComplete();
				}
			}
			if (payloadStream != null) {
				FileOutputStream out = new FileOutputStream(new File("extracted." + result.recordNr), false);
				int read;
				while ((read = payloadStream.read(tmpBuf)) != -1) {
					out.write(tmpBuf, 0, read);
				}
				out.flush();
				out.close();
				payloadStream.close();
			}
			if (httpHeader != null) {
				httpHeader.close();
			}
			if (payload != null) {
				payload.close();
			}
			arcRecord.close();
			++result.recordNr;
		}
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException {
		if (options.targetUri == null || (warcRecord.header.warcTargetUriStr != null && options.targetUri.equalsIgnoreCase(warcRecord.header.warcTargetUriStr))) {
			Payload payload = warcRecord.getPayload();
			HttpHeader httpHeader = null;
			InputStream payloadStream = null;
			if (payload != null) {
				httpHeader = warcRecord.getHttpHeader();
				if (httpHeader != null ) {
					payloadStream = httpHeader.getPayloadInputStream();
				} else {
					payloadStream = payload.getInputStreamComplete();
				}
			}
			if (payloadStream != null) {
				FileOutputStream out = new FileOutputStream(new File("extracted." + result.recordNr), false);
				int read;
				while ((read = payloadStream.read(tmpBuf)) != -1) {
					out.write(tmpBuf, 0, read);
				}
				out.flush();
				out.close();
				payloadStream.close();
			}
			if (httpHeader != null) {
				httpHeader.close();
			}
			if (payload != null) {
				payload.close();
			}
			warcRecord.close();
			++result.recordNr;
		}
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
		result.throwableList.add(new ResultItemThrowable(t, offset, consumed));
	}

	@Override
	public void apcDone(GzipReader gzipReader, ArcReader arcReader, WarcReader warcReader) {
	}

}
