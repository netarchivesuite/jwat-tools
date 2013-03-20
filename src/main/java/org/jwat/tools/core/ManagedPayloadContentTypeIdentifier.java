package org.jwat.tools.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jwat.common.ContentType;

public class ManagedPayloadContentTypeIdentifier {

	/** Thread safe <code>LibmagicContentType</code>. */
    private static final ThreadLocal<ManagedPayloadContentTypeIdentifier> ManagedPayloadContentTypeIdentifierTL = new ThreadLocal<ManagedPayloadContentTypeIdentifier>() {
        @Override
        public ManagedPayloadContentTypeIdentifier initialValue() {
            return new ManagedPayloadContentTypeIdentifier();
        }
    };

    /**
     * Creates a new <code>LibmagicContentType</code> object.
     */
    private ManagedPayloadContentTypeIdentifier() {
    }

    public static ManagedPayloadContentTypeIdentifier getManagedPayloadContentTypeIdentifier() {
        return ManagedPayloadContentTypeIdentifierTL.get();
    }

    /** Bit field used to identify valid first/follow characters. */
    protected static int[] tag_bf = new int[256];

    /*
     * Initialize first/follow bit field.
     */
    static {
        String alphas = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i=0; i<alphas.length(); ++i) {
            tag_bf[alphas.charAt(i)] = 3;
        }
        String digits = "1234567890";
        for (int i=0; i<digits.length(); ++i) {
            tag_bf[digits.charAt(i)] = 2;
        }
    }

	public LibmagicIdentifier libmagicIdentifier = new LibmagicIdentifier();

    protected byte[] buffer = new byte[8192];

	protected ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

    public ManagedPayloadContentType guestimateContentType(ManagedPayload managedPayload) throws IOException {
    	ManagedPayloadContentType managedPayloadContentType = null;
    	InputStream in = null;
    	try {
    		ContentType contentType = libmagicIdentifier.identify(managedPayload);
			boolean bHtml = false;
			int possibleStatusCode = 0;
        	if (contentType != null) {
        		if (!"text".equalsIgnoreCase(contentType.contentType)) {
        			if (contentType.parameters.containsKey("charset")) {
        				contentType.parameters.remove("charset");
        			}
        		} else {
        			String charset = contentType.parameters.get("charset");
        			if (charset.startsWith("unknown-")) {
        				contentType.parameters.remove("charset");
        			}
        			if ("plain".equalsIgnoreCase(contentType.mediaType)) {
        				in = managedPayload.getPayloadStream();
        				byteBuffer.clear();
        				byteBuffer.limit(0);
        				int position = 0;
        				int limit = 0;
        				int remaining;
        				int c;
        				int read;
        				int tmpStatusCode = 0;
        				int numbers = 0;
        				int state = 0;
        				boolean bLoop = true;
        				while (bLoop) {
        					if (position < limit) {
        						c = buffer[position++] & 255;
        						switch (state) {
        						case 0:
        							switch (c) {
        							case ' ':
        								if (possibleStatusCode > 0) {
            								tmpStatusCode = 0;
            								numbers = 0;
            								state = 1;
        								}
        								break;
        							case '<':
        								if (!bHtml) {
            								state = 2;
        								}
        								break;
        							default:
        								break;
        							}
        							break;
        						case 1:
        							switch (c) {
        							case '0':
        							case '1':
        							case '2':
        							case '3':
        							case '4':
        							case '5':
        							case '6':
        							case '7':
        							case '8':
        							case '9':
        								if (numbers < 3) {
            								tmpStatusCode = (tmpStatusCode * 10) + (c - '0');
            								++numbers;
        								} else {
        									state = 0;
        								}
        								break;
        							case ' ':
        								if (numbers == 3) {
        									if (tmpStatusCode == 200 || tmpStatusCode == 404) {
        										possibleStatusCode = tmpStatusCode;
        										bLoop = !bHtml;
        									}
    									}
        								break;
        							default:
        								break;
    								}
        						case 2:
        							if ((tag_bf[c] & 1) != 0) {
        								state = 3;
        							}
        							break;
        						case 3:
        							if ((tag_bf[c] & 2) == 0) {
        								if (c == '/') {
        									state = 4;
        								}
        								if (c == '>') {
        									bHtml = true;
            								bLoop = (possibleStatusCode == 0);
        								}
        							}
        							break;
        						case 4:
    								if (c == '>') {
    									bHtml = true;
        								bLoop = (possibleStatusCode == 0);
    								}
    								state = 0;
        							break;
        						}
        					} else {
                				byteBuffer.clear();
                				position = 0;
                				remaining = byteBuffer.remaining();
                				while ((read = in.read(buffer, position, remaining)) != -1) {
                					position += read;
                					remaining -= read;
                				}
                				byteBuffer.position(position);
                				byteBuffer.flip();
                				position = 0;
                				limit = byteBuffer.limit();
                				bLoop = byteBuffer.hasRemaining();
        					}
        				}
        				if (bHtml) {
        					contentType.mediaType = "html";
        					// debug
        					//System.out.println("-> Promoted to HTML!");
        				}
        			}
        		}
        	}
        	if (contentType != null) {
        		managedPayloadContentType = new ManagedPayloadContentType();
        		managedPayloadContentType.contentType = contentType;
        		managedPayloadContentType.bHtml = bHtml;
        		managedPayloadContentType.possibleStatusCode = possibleStatusCode;
        	}
			// debug
        	//System.out.println(contentType.toString());
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	finally {
    		if (in != null) {
    			in.close();
    		}
    	}
    	return managedPayloadContentType;
    }

}
