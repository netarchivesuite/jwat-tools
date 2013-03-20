package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jwat.arc.ArcHeader;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParser;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;

public class CDXFile implements ArchiveParserCallback {

	protected File srcFile;

	protected String fileName;

	protected List<CDXEntry> entries = new ArrayList<CDXEntry>();

	protected long consumed = 0;

	public CDXFile() {
	}

	public void processFile(File file) {
		fileName = file.getName();
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
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset,
			boolean compressed) throws IOException {
		if (arcRecord.recordType == ArcRecord.RT_ARC_RECORD) {
			CDXEntry entry = new CDXEntry();
			ArcHeader arcHeader = arcRecord.header;
			entry.date = arcHeader.archiveDate;
			entry.ip = arcHeader.ipAddressStr;
			entry.url = arcHeader.urlStr;
			String mimeType = arcHeader.contentTypeStr;
			String responseCode = null;
			ContentType contentType = arcHeader.contentType;
			long length = arcHeader.archiveLength;
			// TODO
	        if (contentType != null
	                && arcHeader.contentType.contentType.equals("application")
	                && arcHeader.contentType.mediaType.equals("http")) {
	            String value = arcHeader.contentType.getParameter("msgtype");
	            HttpHeader httpHeader = arcRecord.getHttpHeader();
	            if ("response".equalsIgnoreCase(value)) {
	            	if (httpHeader != null && httpHeader.contentType != null) {
	            		mimeType = httpHeader.contentType;
	            		responseCode = httpHeader.statusCodeStr;
	            		length = httpHeader.getPayloadLength();
	            	}
	            }
	        }
	        entry.mimetype = mimeType;
	        entry.responseCode = responseCode;
	        entry.checksum = null;
	        entry.offset = startOffset;
	        entry.length = length;
	        entry.fileName = fileName;
	        entries.add(entry);
		}
        arcRecord.close();
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset,
			boolean compressed) throws IOException {
		if (warcRecord.header.warcTypeIdx == WarcConstants.RT_IDX_RESPONSE) {
			CDXEntry entry = new CDXEntry();
			WarcHeader WarcHeader = warcRecord.header;
			entry.date = WarcHeader.warcDate;
			entry.ip = WarcHeader.warcIpAddress;
			entry.url = WarcHeader.warcTargetUriStr;
			String mimeType = WarcHeader.contentTypeStr;
			String responseCode = null;
			ContentType contentType = WarcHeader.contentType;
			long length = WarcHeader.contentLength;
	        if (contentType != null
	                && WarcHeader.contentType.contentType.equals("application")
	                && WarcHeader.contentType.mediaType.equals("http")) {
	            String value = WarcHeader.contentType.getParameter("msgtype");
	            HttpHeader httpHeader = warcRecord.getHttpHeader();
	            if ("response".equalsIgnoreCase(value)) {
	            	if (httpHeader != null && httpHeader.contentType != null) {
	            		mimeType = httpHeader.contentType;
	            		responseCode = httpHeader.statusCodeStr;
	            		length = httpHeader.getPayloadLength();
	            	}
	            }
	        }
	        entry.mimetype = mimeType;
	        entry.responseCode = responseCode;
	        entry.checksum = null;
	        entry.offset = startOffset;
	        entry.length = length;
	        entry.fileName = fileName;
	        entries.add(entry);
		}
		warcRecord.close();
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

	@Override
	public void apcDone() {
	}

}
