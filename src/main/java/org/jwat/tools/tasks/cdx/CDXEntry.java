package org.jwat.tools.tasks.cdx;

import java.util.Date;

import org.apache.commons.httpclient.URIException;
import org.archive.wayback.UrlCanonicalizer;
import org.jwat.arc.ArcDateParser;
import org.jwat.common.Uri;
import org.jwat.common.UriProfile;

/*
cdxOutput.out.println("CDX b e a m s c v n g");
date
ip
url
mimetype
response code
old stylechecksum
v uncompressed arc file offset * 
n arc document length * 
g file name 
*/

// vinavisen.dk/vinavisen/website.nsf/pages/ 20050506142753 http://www.vinavisen.dk/vinavisen/website.nsf/pages/ text/html 200 - - 294494 kb-pligtsystem-44290-20121018212853-00000.warc

// NAS
//b e a m s c v n g

// Wayback-1.4.2
// A b a m s - - v g
// net-bog-klubben.dk/1000028.pdf 20050520084930 http://www.net-bog-klubben.dk/1000028.pdf application/pdf 200 - - 820 kb-pligtsystem-44761-20121107134629-00000.warc

// CDX N b a m s k r M V g
// filedesc:kb-pligtsystem-44761-20121107134629-00000.warc 20121107134629 filedesc:kb-pligtsystem-44761-20121107134629-00000.warc warc/warcinfo0.1.0 - - - - 0 kb-pligtsystem-44761-20121107134629-00000.warc
// net-bog-klubben.dk/1000028.pdf 20050520084930 http://www.net-bog-klubben.dk/1000028.pdf application/pdf 200 - - - 820 kb-pligtsystem-44761-20121107134629-00000.warc

public class CDXEntry {

	public Date date;
	public String ip;
	public String url;
	public String mimetype;
	public String responseCode;
	public String checksum;
	public long offset; 
	public long length; 

	public String toCDXLine(String filename, UrlCanonicalizer canonicalizer, char[] format) {
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
				if (date != null) {
					sb.append(ArcDateParser.getDateFormat().format(date));
				} else {
					sb.append('-');
				}
				break;
			case 'e':
				if (ip != null && ip.length() > 0) {
					sb.append(ip);
				} else {
					sb.append('-');
				}
				break;
			case 'A':
			case 'N':
				if (url != null && url.length() > 0) {
					try {
						sb.append(canonicalizer.urlStringToKey(url));
					}
					catch (URIException e) {
						uri = Uri.create(url, UriProfile.RFC3986_ABS_16BIT_LAX);
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
							sb.append(url.toLowerCase());
						}
					}
				} else {
					sb.append('-');
				}
				break;
			case 'a':
				if (url != null && url.length() > 0) {
					sb.append(url);
				} else {
					sb.append('-');
				}
				break;
			case 'm':
				if (mimetype != null && mimetype.length() > 0) {
					sb.append(mimetype);
				} else {
					sb.append('-');
				}
				break;
			case 's':
				if (responseCode != null && responseCode.length() > 0) {
					sb.append(responseCode);
				} else {
					sb.append('-');
				}
				break;
			case 'c':
				if (checksum != null && checksum.length() > 0) {
					sb.append(checksum);
				} else {
					sb.append('-');
				}
				break;
			case 'v':
			case 'V':
				sb.append(offset);
				break;
			case 'n':
				sb.append(length);
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
