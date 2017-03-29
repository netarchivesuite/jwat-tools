package org.jwat.tools.tasks.cdx;

import org.apache.commons.httpclient.URIException;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.jwat.arc.ArcDateParser;
import org.jwat.common.Uri;
import org.jwat.common.UriProfile;

/**
 * Created by csr on 3/29/17.
 */
public class CDXFormatter {

    public UrlCanonicalizer canonicalizer = new AggressiveUrlCanonicalizer();

    public String cdxEntry(CDXEntry entry, String filename, char[] format) {
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        char c;
        Uri uri;
        String host;
        int port;
        String query;
        for (int i=0; i<format.length; ++i) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            c = format[i];
            switch (c) {
                case 'b':
                    if (entry.date != null) {
                        sb.append(ArcDateParser.getDateFormat().format(entry.date));
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'e':
                    if (entry.ip != null && entry.ip.length() > 0) {
                        sb.append(entry.ip);
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'A':
                case 'N':
                    if (entry.url != null && entry.url.length() > 0) {
                        try {
                            sb.append(canonicalizer.urlStringToKey(entry.url));
                        }
                        catch (URIException e) {
                            uri = Uri.create(entry.url, UriProfile.RFC3986_ABS_16BIT_LAX);
                            StringBuilder cUrl = new StringBuilder();
                            if ("http".equalsIgnoreCase(uri.getScheme())) {
                                host = uri.getHost();
                                port = uri.getPort();
                                query = uri.getRawQuery();
                                if (host.startsWith("www.")) {
                                    host = host.substring("www.".length());
                                }
                                cUrl.append(host);
                                if (port != -1 && port != 80) {
                                    cUrl.append(':');
                                    cUrl.append(port);
                                }
                                cUrl.append(uri.getRawPath());
                                if (query != null) {
                                    cUrl.append('?');
                                    cUrl.append(query);
                                }
                                sb.append(cUrl.toString().toLowerCase());
                            } else {
                                sb.append(entry.url.toLowerCase());
                            }
                        }
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'a':
                    if (entry.url != null && entry.url.length() > 0) {
                        sb.append(entry.url);
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'm':
                    if (entry.mimetype != null && entry.mimetype.length() > 0) {
                        sb.append(entry.mimetype);
                    } else {
                        sb.append('-');
                    }
                    break;
                case 's':
                    if (entry.responseCode != null && entry.responseCode.length() > 0) {
                        sb.append(entry.responseCode);
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'c':
                    if (entry.checksum != null && entry.checksum.length() > 0) {
                        sb.append(entry.checksum);
                    } else {
                        sb.append('-');
                    }
                    break;
                case 'v':
                case 'V':
                    sb.append(entry.offset);
                    break;
                case 'n':
                    sb.append(entry.length);
                    break;
                case 'g':
                    sb.append(filename);
                    break;
                case '-':
                default:
                    sb.append('-');
                    break;
            }
        }
        return sb.toString();
    }

}
