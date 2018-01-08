package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.io.IOException;

import org.jwat.arc.ArcHeader;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ArchiveParser;
import org.jwat.archive.ArchiveParserCallback;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.tools.tasks.ResultItemThrowable;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcRecord;

public class CDXFile implements ArchiveParserCallback {

	public CDXOptions options;

    protected CDXResult result;

	public CDXFile() {
	}

	public CDXResult processFile(File srcFile, CDXOptions options) {
		this.options = options;
		result = new CDXResult();
		result.srcFile = srcFile;
		result.filename = srcFile.getName();
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
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException {
		if (arcRecord.recordType == ArcRecord.RT_ARC_RECORD) {
			CDXEntry entry = new CDXEntry();
			ArcHeader arcHeader = arcRecord.header;
			Long length = arcHeader.archiveLength;
			String mimetype = null;
			String responseCode = "-";
			int idx;
			/*
			 * HttpHeader content-type.
			 */
			HttpHeader httpHeader = arcRecord.getHttpHeader();
        	if (httpHeader != null && httpHeader.contentType != null) {
        		responseCode = httpHeader.statusCodeStr;
        		length = httpHeader.getPayloadLength();
    			String httpContentTypeStr = httpHeader.contentType;
    			ContentType httpContentType = ContentType.parseContentType(httpContentTypeStr);
        		if (httpContentType != null) {
        			httpContentTypeStr = httpContentType.toStringShort();
        		}
        		else {
        			if (httpContentTypeStr != null) {
            			idx = httpContentTypeStr.indexOf(';');
            			if (idx != -1) {
            				httpContentTypeStr = httpContentTypeStr.substring(0, idx);
            			}
            			httpContentTypeStr = httpContentTypeStr.trim();
        			}
        		}
        		mimetype = httpContentTypeStr;
        	}
        	/*
        	 * ArcRecord content-type.
        	 */
        	if (mimetype == null) {
            	ContentType recordContentType = arcHeader.contentType;
            	String recordContentTypeStr = null;
            	if (recordContentType != null) {
            		recordContentTypeStr = recordContentType.toStringShort();
            	}
            	else {
                	recordContentTypeStr = arcHeader.contentTypeStr;
                	if (recordContentTypeStr != null) {
            			idx = recordContentTypeStr.indexOf(';');
            			if (idx != -1) {
            				recordContentTypeStr = recordContentTypeStr.substring(0, idx);
            			}
                	}
        			recordContentTypeStr = recordContentTypeStr.trim();
            	}
            	mimetype = recordContentTypeStr;
        	}
        	/*
        	 * CDX entry values.
        	 */
			entry.date = arcHeader.archiveDate;
			entry.ip = arcHeader.ipAddressStr;
			entry.url = arcHeader.urlStr;
        	entry.mimetype = mimetype;
	        entry.responseCode = responseCode;
	        entry.checksum = null;
	        entry.offset = startOffset;
	        entry.length = length;
	        if (entry.url != null && !entry.url.toLowerCase().startsWith("filedesc:")) {
		        result.entries.add(entry);
	        }
		}
        arcRecord.close();
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException {
		if (warcRecord.header.warcTypeIdx == WarcConstants.RT_IDX_RESPONSE) {
			CDXEntry entry = new CDXEntry();
			WarcHeader warcHeader = warcRecord.header;
			Long length = warcHeader.contentLength;
			String responseCode = "-";
			String mimetype = null;
            String msgtype = null;
            int idx;
			/*
			 * HttpHeader content-type.
			 */
			String recordContentTypeStr = warcHeader.contentTypeStr;
			ContentType recordContentType = warcHeader.contentType;
			if (recordContentType == null) {
				recordContentType = ContentType.parseContentType(recordContentTypeStr);
			}
			if (recordContentType != null) {
				String httpContentTypeStr;
				ContentType httpContentType;
		        if (warcHeader.contentType.contentType.equals("application") && warcHeader.contentType.mediaType.equals("http")) {
		            msgtype = warcHeader.contentType.getParameter("msgtype");
		        }
	            HttpHeader httpHeader = warcRecord.getHttpHeader();
            	if (httpHeader != null && httpHeader.contentType != null) {
            		responseCode = httpHeader.statusCodeStr;
            		length = httpHeader.getPayloadLength();
            		httpContentTypeStr = httpHeader.contentType;
        			httpContentType = ContentType.parseContentType(httpContentTypeStr);
            		if (httpContentType != null) {
            			httpContentTypeStr = httpContentType.toStringShort();
            		}
            		else {
            			if (httpContentTypeStr != null) {
                			idx = httpContentTypeStr.indexOf(';');
                			if (idx != -1) {
                				httpContentTypeStr = httpContentTypeStr.substring(0, idx);
                			}
                			httpContentTypeStr = httpContentTypeStr.trim();
            			}
            		}
            		mimetype = httpContentTypeStr;
            	}
			}
        	/*
        	 * WarcRecord content-type.
        	 */
        	if (mimetype == null) {
            	if (recordContentType != null) {
            		recordContentTypeStr = recordContentType.toStringShort();
            	}
            	else {
            		if (recordContentTypeStr != null) {
            			idx = recordContentTypeStr.indexOf(';');
            			if (idx != -1) {
            				recordContentTypeStr = recordContentTypeStr.substring(0, idx);
            			}
            			recordContentTypeStr = recordContentTypeStr.trim();
            		}
            	}
            	mimetype = recordContentTypeStr;
        	}
        	/*
        	 * CDX entry values.
        	 */
			entry.date = warcHeader.warcDate;
			entry.ip = warcHeader.warcIpAddress;
			entry.url = warcHeader.warcTargetUriStr;
	        entry.mimetype = mimetype;
	        entry.responseCode = responseCode;
	        entry.checksum = null;
	        entry.offset = startOffset;
	        entry.length = length;
	        String warctype = warcHeader.warcTypeStr;
	        if (warctype.equalsIgnoreCase("response") && (msgtype == null || (msgtype != null && msgtype.equalsIgnoreCase("response")))) {
		        result.entries.add(entry);
	        }
		}
		warcRecord.close();
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
		result.throwableList.add(new ResultItemThrowable(t, offset, consumed));
	}

	@Override
	public void apcDone(GzipReader gzipReader, ArcReader arcReader, WarcReader warcReader) {
	}

}
