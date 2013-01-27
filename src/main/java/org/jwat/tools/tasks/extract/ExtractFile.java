package org.jwat.tools.tasks.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParser;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.warc.WarcRecord;

public class ExtractFile implements ArchiveParserCallback {

	protected String fileName;

	protected int recordNr = 1;

	protected byte[] tmpBuf = new byte[8192];

	public ExtractFile() {
	}

	public void processFile(File file) {
		fileName = file.getName();
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = true;
		archiveParser.bPayloadDigestEnabled = true;
		long consumed = archiveParser.parse(file, this);
	}

	@Override
	public void apcFileId(File file, int fileId) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset,
			boolean compressed) throws IOException {
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset,
			boolean compressed) throws IOException {
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
