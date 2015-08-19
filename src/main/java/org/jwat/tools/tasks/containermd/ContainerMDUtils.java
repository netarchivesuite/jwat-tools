package org.jwat.tools.tasks.containermd;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ContainerMDUtils {

	public static final DateFormat RAW_DATE_FORMAT = new MtSafeDateFormat(
			"yyyyMMddHHmmss");
	public static final DateFormat ISO_DATE_FORMAT = new MtSafeDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	public static final long MIN_VALID_DATE = 19700101000000L;

	public static final String CONTAINER_PREFIX = "containerMD";
	public static final String CONTAINER_URI = "http://bibnum.bnf.fr/ns/containerMD-v1";

	public static final String NAME_ATTRIBUTE = "name";
	
	public static boolean isSet(String s) {
		return (s != null && s.trim().length() != 0);
	}

	public static void startElement(StringBuilder sb,
			String name, String[]... attrs) {
		sb.append('<').append(CONTAINER_PREFIX).append(':');
		sb.append(name);
		for (String[] attr : attrs) {
			sb.append(' ').append(attr[0]).append("=\"").append(attr[1])
					.append('"');
		}
		sb.append('>');
	}

	public static void endElement(StringBuilder sb, String name) {
		sb.append("</").append(CONTAINER_PREFIX).append(':');
		sb.append(name).append('>');
	}

	public static void makeElement(StringBuilder sb,
			String name, String value) {
		if (!isSet(value))
			return;
		sb.append("<").append(CONTAINER_PREFIX).append(':');
		sb.append(name).append(">");
		sb.append(encodeContent(value.trim()));
		sb.append("</").append(CONTAINER_PREFIX).append(':');
		sb.append(name).append(">");
	}

	public static void makeElement(StringBuilder sb,
			String name, String value, String[]... attrs) {
		sb.append("<").append(CONTAINER_PREFIX).append(':');
		sb.append(name);
		for (String[] attr : attrs) {
			if (attr[0] != null) {
				sb.append(' ').append(attr[0]);
				sb.append("=\"").append(encodeContent(attr[1])).append('"');
			}
		}
		if (isSet(value)) {
			sb.append('>');
			sb.append(encodeContent(value.trim()));
			sb.append("</").append(CONTAINER_PREFIX).append(':');
			sb.append(name).append(">");
		} else {
			sb.append("/>");
		}
	}

	public static void makeElement(StringBuilder sb,
			String name, String value, Map<ContainerMDAttributeName, Object> attributes) {
		makeElement(sb, name, value, attributes, false);
	}
	
	public static void makeElement(StringBuilder sb,
			String name, String value, Map<ContainerMDAttributeName, Object> attributes, boolean bAttrNameToValue) {
		String elementValue = value;
		
		sb.append("<").append(CONTAINER_PREFIX).append(':');
		sb.append(name);
		for (Entry<ContainerMDAttributeName, Object> e : attributes.entrySet()) {
			ContainerMDAttributeName attrName = e.getKey();
			Object v = e.getValue();
			if (bAttrNameToValue && ContainerMDAttributeName.NAME.equals(attrName)) {
				elementValue = v.toString();
				continue;
			}
			if (v != null && (!"".equals(v))) {
				sb.append(' ').append(attrName);
				sb.append("=\"").append(encodeContent(v.toString())).append('"');
			}
		}
		if (isSet(elementValue)) {
			sb.append('>');
			sb.append(encodeContent(elementValue.trim()));
			sb.append("</").append(CONTAINER_PREFIX).append(':');
			sb.append(name).append(">");
		} else {
			sb.append("/>");
		}
	}

	/**
	 * Encodes a content String in XML-clean form, converting characters to
	 * entities as necessary. The null string will be converted to an empty
	 * string.
	 */
	public static String encodeContent(String content) {
		if (!isSet(content)) {
			return "";
		}
		final StringBuilder result = new StringBuilder(content.length() * 2);
		final StringCharacterIterator it = new StringCharacterIterator(content);
		char ch = it.current();
		while (ch != CharacterIterator.DONE) {
			if (ch == '&') {
				result.append("&amp;");
			} else if (ch == '<') {
				result.append("&lt;");
			} else if (ch == '>') {
				result.append("&gt;");
			} else if (ch == '\'') {
				result.append("&apos;");
			} else if (ch == '"') {
				result.append("&quot;");
			} else if (Character.isISOControl(ch)) {
				if (Character.isWhitespace(ch)) {
					result.append(String.format("&#%03d;", (int)ch));
				} else {
					result.append(String.format("0x%x", (int)ch));
				}
			} else {
				result.append(ch);
			}
			ch = it.next();
		}
		return result.toString();
	}

	public static void prettyPrintXml(String unformattedXml, PrintStream output) {
		try {
			// Parse the XML string
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();

			final Document doc = db.parse(new InputSource(new StringReader(
					unformattedXml)));

			// Make a identity transformation
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			try {
				transformer.setOutputProperty(
						"{http://xml.apache.org/xslt}indent-amount", "2");
			} catch (IllegalArgumentException e) {
				/* IGNORE works only with Apache */
			}
			// initialize StreamResult with output to save to file
			StreamResult result = new StreamResult(output);
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	final static class MtSafeDateFormat extends SimpleDateFormat {
		private static final long serialVersionUID = 1L;

		public MtSafeDateFormat(String pattern) {
			super(pattern);
			this.setLenient(false);
			this.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		@Override
		public synchronized Date parse(String source) throws ParseException {
			return super.parse(source);
		}

		@Override
		public synchronized StringBuffer format(Date date,
				StringBuffer toAppendTo, FieldPosition pos) {
			return super.format(date, toAppendTo, pos);
		}
	}

	public static long verifyDate(String dateTime) {
		long dateInSeconds = -1L;
		if (!isSet(dateTime)) {
			return dateInSeconds;
		}
		try {
			// Date as long in ARC files
			dateInSeconds = Long.parseLong(dateTime);
		} catch (NumberFormatException e1) {
			// Date as ISO in WARC files
			try {
				Date d = ISO_DATE_FORMAT.parse(dateTime);
				dateInSeconds = Long.parseLong(RAW_DATE_FORMAT.format(d));
			} catch (ParseException e2) {
				dateInSeconds = -1L;
			}
		}
		return dateInSeconds;
	}

	/**
	 * Formats a given long "yyyyMMddHHmmss" into a date
	 * 
	 * @param date
	 * @return Long
	 */
	public static Date longToDate(long date) throws ParseException {
		Date d = null;
		if (date >= MIN_VALID_DATE) {
			d = RAW_DATE_FORMAT.parse(String.valueOf(date));
		}
		return d;
	}

	/**
	 * Formats a given date into a long "yyyyMMddHHmmss"
	 * 
	 * @param date
	 * @return Long
	 */
	public static Long dateToLong(Date date) throws ParseException {
		String stringDate = RAW_DATE_FORMAT.format(date);
		return Long.valueOf(stringDate);
	}

	/**
	 * Formats a given date into "yyyy-MM-dd'T'HH:mm:ss'Z'".
	 * 
	 * @param DateTime
	 * @return <code>String</code>
	 * @throws ParseException
	 */
	public static String formatDateTime(Date date) throws ParseException {
		if (date == null) {
			throw new IllegalArgumentException("Invalid date: " + date);
		}
		return ISO_DATE_FORMAT.format(date);
	}

	final static Pattern MIMETYPE_FORMAT = Pattern
			.compile("^[\\-\\.\\+\\w]+/[\\-\\.\\+\\w]+$");
	final static String UNKNOWN_MIMETYPE = "application/x-unknown-content-type";

	/**
	 * Control that the String given can represent a mimetype. Returns null if
	 * given null, UNKNOWN_MIMETYPE if the mimetype is not parsable
	 * 
	 * @param mimeType
	 * @return a controled mimetype
	 */
	public static String verifyMimeType(String mimeType) {
		if (!isSet(mimeType)) {
			return null;
		}
		int indexMT = mimeType.indexOf(";");
		if (indexMT != -1) {
			mimeType = mimeType.substring(0, indexMT).trim();
		}
		Matcher m2 = MIMETYPE_FORMAT.matcher(mimeType);
		String controledMimeType = UNKNOWN_MIMETYPE;
		if (m2.matches()) {
			controledMimeType = mimeType;
		} else {
			// Problem with mimetype [" + mimeType + "]"
		}
		return controledMimeType;
	}

	final static Pattern HOST_EXTRACTOR = Pattern
			.compile("^([a-zA-Z]*):/{0,3}([.[^/#?:]]*)(?:.*)");

}
