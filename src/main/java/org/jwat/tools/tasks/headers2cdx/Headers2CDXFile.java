package org.jwat.tools.tasks.headers2cdx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import org.jwat.arc.ArcFieldParsers;
import org.jwat.arc.ArcHeader;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.Diagnosis;
import org.jwat.common.Diagnostics;
import org.jwat.common.HeaderLineReader;
import org.jwat.common.HttpHeader;
import org.jwat.common.UriProfile;
import org.jwat.tools.core.IOUtils;
import org.jwat.tools.core.ThreadLocalObjectPool;
import org.jwat.tools.tasks.cdx.CDXEntry;
import org.jwat.tools.tasks.compress.RecordEntry;
import org.jwat.tools.tasks.headers2cdx.JSONDeserializer.JSONDeserializerFactory;
import org.jwat.warc.WarcFieldParsers;
import org.jwat.warc.WarcHeader;

public class Headers2CDXFile {

	private static ThreadLocalObjectPool<JSONDeserializer> jsonTLPool;

	static {
		jsonTLPool = ThreadLocalObjectPool.getPool(new JSONDeserializerFactory());
	}

	protected Headers2CDXResult processFile(File srcFile) {
		Diagnostics<Diagnosis> diagnostics = new Diagnostics<Diagnosis>(); 
		ArcFieldParsers arcFieldParsers = new ArcFieldParsers();
		arcFieldParsers.diagnostics = diagnostics;
		UriProfile uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
		WarcFieldParsers warcFieldParsers = new WarcFieldParsers();

        int recordHeaderMaxSize = 8192;
        HeaderLineReader lineReader = HeaderLineReader.getReader();
        lineReader.bNameValue = false;
        lineReader.encoding = HeaderLineReader.ENC_US_ASCII;
        HeaderLineReader headerLineReader = HeaderLineReader.getReader();
        headerLineReader.bNameValue = true;
        headerLineReader.encoding = HeaderLineReader.ENC_UTF8;
        headerLineReader.bLWS = true;
        headerLineReader.bQuotedText = true;
        headerLineReader.bEncodedWords = true;

		JSONDeserializer jser = null;
    	RecordEntry recordEntry;
    	Headers2CDXResult result = new Headers2CDXResult();
    	result.srcFile = srcFile;

    	String filename = srcFile.getName();
    	if (filename.endsWith(".headers.gz")) {
    		filename = filename.substring(0, filename.length() - ".headers.gz".length());
    	}
    	result.filename = filename;

    	ByteArrayInputStream bIn;
		ByteCountingPushBackInputStream bcpbin;
		Long length;
		ArcHeader arcHeader;
		WarcHeader warcHeader;
		HttpHeader httpHeader;

		try {
        	jser = jsonTLPool.getThreadLocalObject();
        	jser.open(srcFile);

        	while ((recordEntry = jser.deserialize()) != null) {
    	        if (recordEntry.ah != null) {
    	        	// ARC header.
    	        	bIn = new ByteArrayInputStream(recordEntry.ah);
    	    		arcHeader = ArcHeader.initHeader(arcFieldParsers, uriProfile, diagnostics);
    	    		bcpbin = new ByteCountingPushBackInputStream(bIn, 1024);
    	    		arcHeader.parseHeader(bcpbin);
    	    		bcpbin.close();
    	    		bIn.close();
    	    		// HTTP header.
            		httpHeader = null;
            		if (recordEntry.ht != null && recordEntry.hh != null) {
        	        	bIn = new ByteArrayInputStream(recordEntry.hh);
        	    		bcpbin = new ByteCountingPushBackInputStream(bIn, 1024);
        	    		length = arcHeader.archiveLength;
        	    		if (length == null) {
        	    			try {
        	    				length = Long.parseLong(arcHeader.archiveLengthStr);
        	    			}
        	    			catch (NumberFormatException e) {
        	    			}
        	    		}
        	    		if (length == null || (long)recordEntry.hh.length > length) {
        	    			length = (long)recordEntry.hh.length;
        	    		}
            			httpHeader = HttpHeader.processPayload(recordEntry.ht, bcpbin, length, null);
        	    		bcpbin.close();
        	    		bIn.close();
            		}
    	    		cdxArcRecord(arcHeader, httpHeader, recordEntry, result.entries);
    	        }
    	        /*
    	        if (recordEntry.aL != null) {
    	    		arcHeader = recordEntry.NameValueListToArcHeader(arcFieldParsers, uriProfile, diagnostics, recordEntry.aL);
    	        }
    	        */
    	        if (recordEntry.wh != null) {
    	        	// WARC header.
    	        	bIn = new ByteArrayInputStream(recordEntry.wh);
    	    		warcHeader = WarcHeader.initHeader(recordHeaderMaxSize, lineReader, headerLineReader, warcFieldParsers, uriProfile, diagnostics);
    	    		bcpbin = new ByteCountingPushBackInputStream(bIn, 1024);
    	    		warcHeader.parseHeader(bcpbin);
    	    		bcpbin.close();
    	    		bIn.close();
    	    		// HTTP header.
            		httpHeader = null;
            		if (recordEntry.ht != null && recordEntry.hh != null) {
            			// debug
            			//System.out.println(new String(recordEntry.hh));
        	        	bIn = new ByteArrayInputStream(recordEntry.hh);
        	    		bcpbin = new ByteCountingPushBackInputStream(bIn, 1024);
        	    		length = warcHeader.contentLength;
        	    		if (length == null) {
        	    			try {
        	    				length = Long.parseLong(warcHeader.contentLengthStr);
        	    			}
        	    			catch (NumberFormatException e) {
        	    			}
        	    		}
        	    		if (length == null || (long)recordEntry.hh.length > length) {
        	    			length = (long)recordEntry.hh.length;
        	    		}
            			httpHeader = HttpHeader.processPayload(recordEntry.ht, bcpbin, length, null);
            			if (!httpHeader.isValid()) {
                			// debug
                			//System.out.println(httpHeader.isValid());
                			System.out.println(new String(recordEntry.hh));
            			}
        	    		bcpbin.close();
        	    		bIn.close();
            		}
    	    		cdxWarcRecord(warcHeader, httpHeader, recordEntry, result.entries);
    	        }
    	        /*
    	        if (recordEntry.wL != null) {
    	    		warcHeader = recordEntry.NameValueListToWarcHeader(1, 0, warcFieldParsers, uriProfile, diagnostics, recordEntry.wL);
    	        }
    	        */
        	}

        	jser.close();
        	jser = null;

        	result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
			IOUtils.closeIOQuietly(jser);
		}
   		return result;
	}

	public void cdxArcRecord(ArcHeader arcHeader, HttpHeader httpHeader, RecordEntry recordEntry, List<CDXEntry> entries) {
		CDXEntry entry = new CDXEntry();
		long startOffset = recordEntry.i;
		long length = recordEntry.l;
		//long length = arcHeader.archiveLength;
		String mimetype = null;
		String responseCode = "-";
		int idx;
		/*
		 * HttpHeader content-type.
		 */
		//HttpHeader httpHeader = arcRecord.getHttpHeader();
    	if (httpHeader != null && httpHeader.contentType != null) {
    		responseCode = httpHeader.statusCodeStr;
    		//length = httpHeader.getPayloadLength();
    		length = httpHeader.payloadLength;
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
	        entries.add(entry);
        }
	}

	public void cdxWarcRecord(WarcHeader warcHeader, HttpHeader httpHeader, RecordEntry recordEntry, List<CDXEntry> entries) {
		CDXEntry entry = new CDXEntry();
		long startOffset = recordEntry.i;
		long length = recordEntry.l;
		//long length = warcHeader.contentLength;
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
            //HttpHeader httpHeader = warcRecord.getHttpHeader();
        	if (httpHeader != null && httpHeader.contentType != null) {
        		responseCode = httpHeader.statusCodeStr;
        		//length = httpHeader.getPayloadLength();
        		length = httpHeader.payloadLength;
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
	        entries.add(entry);
        }

	}

}
