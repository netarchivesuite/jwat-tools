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

	protected List<CDXEntry> entries = new ArrayList<CDXEntry>();

	protected String fileName;

	public CDXFile() {
	}

	public List<CDXEntry> processFile(File file) {
		fileName = file.getName();
		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		archiveParser.bBlockDigestEnabled = true;
		archiveParser.bPayloadDigestEnabled = true;
		long consumed = archiveParser.parse(file, this);
		return entries;
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
			ArcHeader header = arcRecord.header;
			entry.date = header.archiveDate;
			entry.ip = header.ipAddressStr;
			entry.url = header.urlStr;
			String mimeType = header.contentTypeStr;
			String responseCode = null;
			ContentType contentType = header.contentType;
			long length = header.archiveLength;
	        if (contentType != null
	                && header.contentType.contentType.equals("application")
	                && header.contentType.mediaType.equals("http")) {
	            String value = header.contentType.getParameter("msgtype");
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
			WarcHeader header = warcRecord.header;
			entry.date = header.warcDate;
			entry.ip = header.warcIpAddress;
			entry.url = header.warcTargetUriStr;
			String mimeType = header.contentTypeStr;
			String responseCode = null;
			ContentType contentType = header.contentType;
			long length = header.contentLength;
	        if (contentType != null
	                && header.contentType.contentType.equals("application")
	                && header.contentType.mediaType.equals("http")) {
	            String value = header.contentType.getParameter("msgtype");
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
