package org.jwat.tools.tasks.arc2warc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.jwat.arc.ArcConstants;
import org.jwat.archive.ManagedPayload;
import org.jwat.common.ArrayUtils;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.jwat.tools.core.ManagedPayloadContentType;
import org.jwat.tools.core.ManagedPayloadContentTypeIdentifier;

public class RepairPayload {

	/** Thread safe <code>RepairPayload</code>. */
    private static final ThreadLocal<RepairPayload> RepairPayloadTL = new ThreadLocal<RepairPayload>() {
        @Override
        public RepairPayload initialValue() {
            return new RepairPayload();
        }
    };

    //private Calendar calendar;
    private SimpleDateFormat dateFormat;

    /**
     * Creates a new <code>RepairPayload</code> object.
     */
    private RepairPayload() {
        //calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static RepairPayload getRepairPayload() {
        return RepairPayloadTL.get();
    }

    private byte[] tmpBuf = new byte[16384];

	private int position = 0;

	private int limit = 0;

	private PushbackInputStream pbin;

	private ByteArrayOutputStream httpOut = new ByteArrayOutputStream();

	private boolean bRepaired = false;

	public ManagedPayload repairPayload(ManagedPayload managedPayload, String contentTypeStr, Date date) throws IOException {
    	ContentType contentType = null;
    	int possibleStatusCode = 0;

    	position = 0;
    	limit = 0;
    	pbin = null;
    	httpOut.reset();
    	bRepaired = false;

    	InputStream payloadStream = null;
    	HttpHeader httpHeader;
    	ManagedPayload newManagedPayload;
    	long newPayloadLength;
    	try {
    		/*
    		 * no-type -> libmagic identify.
    		 */
        	if (ArcConstants.CONTENT_TYPE_NO_TYPE.equalsIgnoreCase(contentTypeStr)) {
            	ManagedPayloadContentTypeIdentifier managedPayloadContentTypeIdentifier = ManagedPayloadContentTypeIdentifier.getManagedPayloadContentTypeIdentifier();
            	ManagedPayloadContentType managedPayloadContentType = managedPayloadContentTypeIdentifier.guestimateContentType(managedPayload);
            	if (managedPayloadContentType != null) {
            		contentType = managedPayloadContentType.contentType;
            		possibleStatusCode = managedPayloadContentType.possibleStatusCode;
            	}
        	} else {
        		contentType = ContentType.parseContentType(contentTypeStr);
        	}
        	if (contentType == null) {
        		System.out.println("Unknown: " + contentTypeStr);
        	}
        	if (contentType != null) {
            	if ("text".equalsIgnoreCase(contentType.contentType)) {
                	/*
                	 * Read first 16K so we can see what can be repaired.
                	 */
                	payloadStream = managedPayload.getPayloadStream();
                	pbin = new PushbackInputStream(payloadStream, 16384);
                	position = 0;
                	int remaining = tmpBuf.length;
                	int read = 0;
                	while (remaining > 0 && read != -1) {
                		read = pbin.read(tmpBuf, position, remaining);
                		if (read > 0) {
                    		position += read;
                    		remaining -= read;
                		}
                	}
                	limit = position;
                	position = 0;

                	int position2;

                	byte[] CRLFCRLF = "\r\n\r\n".getBytes();
                	byte[] fail_match1 = "HTTP/1.0 404: Not found\r\n\r\n".getBytes();
                	byte[] fail_match2 = "HTTP/1.1 /images/head_lycos_search.gif\r\n".getBytes();
                	byte[] fail_match3 = "http/1.0 301 redirect\r\n".getBytes();

                	if (ArrayUtils.startsWith(fail_match1, tmpBuf)) {
                		pbin.unread(tmpBuf, fail_match1.length, limit - fail_match1.length);

                		newPayloadLength = managedPayload.payloadLength - fail_match1.length;

                		httpOut.reset();
                		httpOut.write("HTTP/1.0 404 Not found".getBytes());
                		httpOut.write("\r\n".getBytes());
                		if (date != null) {
                    		httpOut.write("Date: ".getBytes());
                    		httpOut.write(dateFormat.format(date).getBytes());
                    		httpOut.write("\r\n".getBytes());
                		}
                		httpOut.write("Content-Length: ".getBytes());
                		httpOut.write(Long.toString(newPayloadLength).getBytes());
                		httpOut.write("\r\n".getBytes());
                		if (contentType != null) {
                    		httpOut.write("Content-Type: ".getBytes());
                    		httpOut.write(contentType.toString().getBytes());
                    		httpOut.write("\r\n".getBytes());
                		}
                		httpOut.write("Connection: close".getBytes());
                		httpOut.write("\r\n".getBytes());
                		httpOut.write("\r\n".getBytes());

                		newManagedPayload = ManagedPayload.checkout();
                		newManagedPayload.managedHttp(httpOut.toByteArray(), true);
                		newManagedPayload.managePayloadInputStream(pbin, newPayloadLength, true);
                		managedPayload.checkin();
                		managedPayload = newManagedPayload;

                		System.out.println("case 1");
                	} else if (ArrayUtils.startsWith(fail_match2, tmpBuf)) {
                		position = fail_match2.length;
                		position2 = ArrayUtils.indexOf(CRLFCRLF, tmpBuf, position);

                		httpOut.reset();
                		httpOut.write("HTTP/1.1 302 Found\r\n".getBytes());
                		httpOut.write(tmpBuf, position, position2 - position);
                		byte[] httpHeaderBytes = httpOut.toByteArray();

                		httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE,
                				new ByteCountingPushBackInputStream(new ByteArrayInputStream(httpHeaderBytes), 8192),
                				httpHeaderBytes.length,
                				null);

                		if (httpHeader != null && httpHeader.isValid()) {
                    		pbin.unread(tmpBuf, position2, limit - position2);

                    		newPayloadLength = managedPayload.payloadLength - position2;

                    		newManagedPayload = ManagedPayload.checkout();
                    		newManagedPayload.managedHttp(httpHeaderBytes, true);
                    		newManagedPayload.managePayloadInputStream(pbin, newPayloadLength, true);
                    		managedPayload.checkin();
                    		managedPayload = newManagedPayload;

                			System.out.println("case 2");
                		} else {
                			System.out.println("fail case 2");
                		}
                	} else if (ArrayUtils.startsWith(fail_match3, tmpBuf)) {
                		position = fail_match3.length;
                		position2 = ArrayUtils.indexOf(CRLFCRLF, tmpBuf, position);

                		httpOut.reset();
                		httpOut.write("HTTP/1.0 301 Redirect\r\n".getBytes());
                		httpOut.write(tmpBuf, position, position2 - position);
                		byte[] httpHeaderBytes = httpOut.toByteArray();

                		httpHeader = HttpHeader.processPayload(HttpHeader.HT_RESPONSE,
                				new ByteCountingPushBackInputStream(new ByteArrayInputStream(httpHeaderBytes), 8192),
                				httpHeaderBytes.length,
                				null);

                		if (httpHeader != null && httpHeader.isValid()) {
                    		pbin.unread(tmpBuf, position2, limit - position2);

                    		newPayloadLength = managedPayload.payloadLength - position2;

                    		newManagedPayload = ManagedPayload.checkout();
                    		newManagedPayload.managedHttp(httpHeaderBytes, true);
                    		newManagedPayload.managePayloadInputStream(pbin, newPayloadLength, true);
                    		managedPayload.checkin();
                    		managedPayload = newManagedPayload;

                			System.out.println("case 3");
                		} else {
                			System.out.println("fail case 3");
                		}
                	} else {
            			position = ArrayUtils.skip(ArrayUtils.SKIP_WHITESPACE,tmpBuf, 0);
            			if (position < limit) {
            				if (!bRepaired) {
            					managedPayload = tryrepair_insert_200(managedPayload, contentType, date);
                    		}
            				if (!bRepaired) {
            					managedPayload = tryrepair_insert_404(managedPayload, contentType, date);
            				}
            				if (!bRepaired) {
            					managedPayload = tryrepair_insert_500(managedPayload, contentType, date);
            				}
            				/*
            				if (!bRepaired) {
                        		byte[][] htmlTags = {
                                		"Location: http://".getBytes()
                        		};
                				idx = 0;
                				while (!bInsertHttpHeader && idx < htmlTags.length) {
                    				bInsertHttpHeader = ArrayUtils.equalsAtIgnoreCase(htmlTags[idx], tmpBuf, position);
                    				++idx;
                				}
            				}
            				*/
                		} else {
                			// All spaces are belong in tmpBuf...
                    		System.out.println("case 0");
                		}
                	}
                	//String statusLine = "HTTP/1.1 " + possibleStatusCode + " ";
            	} else if (contentType != null) {
                	payloadStream = managedPayload.getPayloadStream();
                	pbin = new PushbackInputStream(payloadStream, 16384);

                	newPayloadLength = managedPayload.payloadLength;

                	managedPayload = insertHeader(managedPayload, newPayloadLength, "HTTP/1.1 200 OK", contentType, date);
            	}
        	}
    	} finally {
    		if (payloadStream != null) {
            	payloadStream.close();
            	payloadStream = null;
    		}
    	}
    	return managedPayload;
    }

	public ManagedPayload insertHeader(ManagedPayload managedPayload, long newPayloadLength, String statusLine, ContentType contentType, Date date) throws IOException {
		httpOut.reset();
		httpOut.write(statusLine.getBytes());
		httpOut.write("\r\n".getBytes());
		if (date != null) {
    		httpOut.write("Date: ".getBytes());
    		httpOut.write(dateFormat.format(date).getBytes());
    		httpOut.write("\r\n".getBytes());
		}
		httpOut.write("Content-Length: ".getBytes());
		httpOut.write(Long.toString(newPayloadLength).getBytes());
		httpOut.write("\r\n".getBytes());
		if (contentType != null) {
    		httpOut.write("Content-Type: ".getBytes());
    		httpOut.write(contentType.toString().getBytes());
    		httpOut.write("\r\n".getBytes());
		}
		httpOut.write("Connection: close".getBytes());
		httpOut.write("\r\n".getBytes());
		httpOut.write("\r\n".getBytes());

		ManagedPayload newManagedPayload = ManagedPayload.checkout();
		newManagedPayload.managedHttp(httpOut.toByteArray(), true);
		newManagedPayload.managePayloadInputStream(pbin, newPayloadLength, true);
		managedPayload.checkin();
		managedPayload = newManagedPayload;
		return managedPayload;
	}

	public ManagedPayload tryrepair_insert_200(ManagedPayload managedPayload, ContentType contentType, Date date) throws IOException {
    	byte[][] cases = {
    			"<!DOCTYPE".getBytes(),
    			"<HTML".getBytes(),
    			"<HEAD>".getBytes(),
    			"<META ".getBytes(),
    			"<body>".getBytes(),
    			"<body ".getBytes(),
    			"<script>".getBytes(),
    			"<script language=".getBytes(),
    			"<font face=\"".getBytes(),
    			"<img src=\"".getBytes(),
    			"<center>".getBytes(),
    			">".getBytes(),
    			"<!--".getBytes(),
    			(
    					"SSH-2.0-OpenSSH_3.6.1p1\n"
						+ "Protocol mismatch.\n"
    			).getBytes(),
    			"document.write(\"\\n\");".getBytes(),
    			"ERROR 425: Unable to connect with remote host.".getBytes(),
    			"Message: ".getBytes(),
    			"User-agent: *".getBytes(),
    			"# Nearly all robots are welcomed!".getBytes(),
    			"Open Proxy Check".getBytes(),
    			"You are banned from this server! (LiteServe v2.2 - www.liteserve.net)".getBytes()
		};
		long newPayloadLength;
		boolean bInsertHttpHeader = false;
		int idx = 0;
		while (!bInsertHttpHeader && idx < cases.length) {
			bInsertHttpHeader = ArrayUtils.equalsAtIgnoreCase(cases[idx], tmpBuf, position);
			++idx;
		}
		if (bInsertHttpHeader) {
    		pbin.unread(tmpBuf, 0, limit);

    		newPayloadLength = managedPayload.payloadLength;

    		managedPayload = insertHeader(managedPayload, newPayloadLength, "HTTP/1.1 200 OK", contentType, date);

    		System.out.println("case 4");
    		bRepaired = true;
		}
		return managedPayload;
    }

    public ManagedPayload tryrepair_insert_404(ManagedPayload managedPayload, ContentType contentType, Date date) throws IOException {
		byte[][] cases = {
        		"ICY 404 Resource Not Found".getBytes(),
        		"Can't open perl script \"".getBytes(),
        		"Sorry file not found (in 404)".getBytes()
		};
		long newPayloadLength;
		boolean bInsertHttpHeader = false;
		int idx = 0;
		while (!bInsertHttpHeader && idx < cases.length) {
			bInsertHttpHeader = ArrayUtils.equalsAtIgnoreCase(cases[idx], tmpBuf, position);
			++idx;
		}
		if (bInsertHttpHeader) {
    		pbin.unread(tmpBuf, 0, limit);

    		newPayloadLength = managedPayload.payloadLength;

    		managedPayload = insertHeader(managedPayload, newPayloadLength, "HTTP/1.1 404 Not found", contentType, date);

    		System.out.println("case 5");
    		bRepaired = true;
		}
    	return managedPayload;
    }

    public ManagedPayload tryrepair_insert_500(ManagedPayload managedPayload, ContentType contentType, Date date) throws IOException {
    	byte[][] cases = {
    			"PHP has encountered an Access Violation at 10001364".getBytes(),
    			"Error executing ACGI application. No results returned.".getBytes(),
    			"Error receiving results from ACGI execution. (-1701)".getBytes()
    	};
		long newPayloadLength;
		boolean bInsertHttpHeader = false;
		int idx = 0;
		while (!bInsertHttpHeader && idx < cases.length) {
			bInsertHttpHeader = ArrayUtils.equalsAtIgnoreCase(cases[idx], tmpBuf, position);
			++idx;
		}
		if (bInsertHttpHeader) {
    		pbin.unread(tmpBuf, 0, limit);

    		newPayloadLength = managedPayload.payloadLength;

    		managedPayload = insertHeader(managedPayload, newPayloadLength, "HTTP/1.1 500 Internal Server Error", contentType, date);

    		System.out.println("case 5");
    		bRepaired = true;
		}
    	return managedPayload;
    }

}
