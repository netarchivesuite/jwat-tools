package org.jwat.tools.tasks.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ArchiveParser;
import org.jwat.archive.ArchiveParserCallback;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.warc.WarcRecord;

public class ExtractFile implements ArchiveParserCallback {

	protected File srcFile;

	protected String fileName;

	protected String targetUri;

	protected int recordNr = 1;

	protected byte[] tmpBuf = new byte[8192];

	protected long consumed = 0;

	public ExtractFile() {
	}

	public void processFile(File file, String targetUri) {
		fileName = file.getName();
		this.targetUri = targetUri;
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = true;
		archiveParser.bPayloadDigestEnabled = true;
		consumed = archiveParser.parse(file, this);
	}

	@Override
	public void apcFileId(File file, int fileId) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException {
		if (targetUri == null || targetUri.equalsIgnoreCase(arcRecord.header.urlStr)) {
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
				FileOutputStream out = new FileOutputStream(new File("extracted." + recordNr), false);
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
			++recordNr;
		}
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException {
		if (targetUri == null || (warcRecord.header.warcTargetUriStr != null && targetUri.equalsIgnoreCase(warcRecord.header.warcTargetUriStr))) {
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
				FileOutputStream out = new FileOutputStream(new File("extracted." + recordNr), false);
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
			++recordNr;
		}
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

	@Override
	public void apcDone() {
	}

}
