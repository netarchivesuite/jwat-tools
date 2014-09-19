package org.jwat.tools.tasks.containermd;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/**
 * A wrapper for the data needed to build the containerMD section of the
 * manifest. Data are gathered during the parse of the archive file and
 * aggregated.
 */
public final class ContainerMDWrapper {
	public SortedMap<String, ContainerMDElement> encodings;
	public SortedMap<String, ContainerMDElement> blockFormats;
	public SortedMap<String, ContainerMDElement> payloadFormats;
	public SortedMap<String, ContainerMDElement> declaredMimeTypes;
	public SortedMap<String, ContainerMDElement> hosts;
	public SortedMap<String, ContainerMDElement> responses;

	private long number = 0;
	private long firstDateTime = -1L;
	private long lastDateTime = -1L;
	
	private long minimumSize = Long.MAX_VALUE;
	private long maximumSize = 0L;
	private long globalSize = 0L;

	/**
	 * Creates a new ContainerMDWrapper instance.
	 */
	public ContainerMDWrapper() {
		this.encodings = new TreeMap<String, ContainerMDElement>();

		this.blockFormats = new TreeMap<String, ContainerMDElement>();
		this.payloadFormats = new TreeMap<String, ContainerMDElement>();
		this.declaredMimeTypes = new TreeMap<String, ContainerMDElement>();
		this.hosts = new TreeMap<String, ContainerMDElement>();
		this.responses = new TreeMap<String, ContainerMDElement>();
	}

	/**
	 * Register a new entry of an ARC/WARC file
	 * 
	 * @param sourceName
	 * @param size
	 * @param dateTime
	 * @param format
	 * @param mimeType
	 * @param protocolVersion
	 * @param codeResponse
	 */
	public void addEntry(String sourceName, long size, String dateTime,
			String blockFormat, String declaredMimeType, String payloadFormat,
			String protocolVersion,
			String codeResponse) {
		number++;
		if (size >= 0L) {
			this.setMaximumSize(size);
			this.setMinimumSize(size);
			this.setGlobalSize(size);
		}

		long dateInSeconds = ContainerMDUtils.verifyDate(dateTime);
		if (dateInSeconds != -1L) {
			this.setFirstDateTime(dateInSeconds);
			this.setLastDateTime(dateInSeconds);
		}
		// Handle the sourceName
		if (ContainerMDUtils.isSet(sourceName)) {
			Matcher m = ContainerMDUtils.HOST_EXTRACTOR.matcher(sourceName);
			if (m.matches()) {
				String protocol = m.group(1);
				String hostName = m.group(2);
				int index = hostName.lastIndexOf('@');
				if (index != -1) {
					// hostname with @, keep only last part
					hostName = hostName.substring(index + 1);
				}
				if (hostName.contains("&")) {
					// hostname with &, replace with %26
					hostName = hostName.replaceAll("&", "%26");
				}

				this.handleHost(hostName, size);
				this.handleResponse(protocolVersion, protocol, codeResponse,
						size);
			}
		}

		// Handle the mimetype
		this.handleBlockFormat(ContainerMDUtils.verifyMimeType(blockFormat), size);
		this.handleDeclaredMimeType(ContainerMDUtils.verifyMimeType(declaredMimeType), size);
		this.handlePayloadFormat(payloadFormat, size);
	}

	/**
	 * Gets number of records
	 * 
	 * @return <code>long</code>
	 */
	public long getNumber() {
		return number;
	}

	/**
	 * Sets maximum size
	 * 
	 * @param size
	 */
	public void setMaximumSize(long size) {
		if (this.maximumSize < size) {
			this.maximumSize = size;
		}
	}

	/**
	 * Gets maximumSize
	 * 
	 * @return <code>String</code>
	 */
	public String getMaximumSize() {
		return Long.toString(this.maximumSize);
	}

	/**
	 * Sets minimum size
	 * 
	 * @param size
	 */
	public void setMinimumSize(long size) {
		if ((size >= 0L) && (this.minimumSize > size)) {
			this.minimumSize = size;
		}
	}

	/**
	 * Sets the global size
	 * 
	 * @param size
	 */
	public void setGlobalSize(long size) {
		if (size >= 0L) {
			this.globalSize += size;
		}
	}

	/**
	 * Gets the global size
	 * 
	 * @return <code>long</code>
	 */
	public long getGlobalSize() {
		return this.globalSize;
	}

	/**
	 * Gets minimumSize
	 * 
	 * @return <code>String</code>
	 */
	public String getMinimumSize() {
		return Long.toString((this.minimumSize == Long.MAX_VALUE) ? 0L
				: this.minimumSize);
	}

	/**
	 * Sets firstLastTime
	 * 
	 * @param dateTime
	 */
	public void setFirstDateTime(long dateTime) {
		if ((this.firstDateTime == -1L) || (this.firstDateTime > dateTime)) {
			this.firstDateTime = dateTime;
		}
	}

	/**
	 * Gets firstDateTime
	 * 
	 * @return <code>String</code>
	 */
	public String getFirstDateTime() {
		if (this.firstDateTime == -1L)
			return "";
		String dt = "";
		try {
	        dt = ContainerMDUtils.formatDateTime(ContainerMDUtils
	        		.longToDate(this.firstDateTime));
        } catch (ParseException e) {
        	dt = "";
        }
		return dt;
	}

	/**
	 * Sets lastDateTime
	 * 
	 * @param dateTime
	 */
	public void setLastDateTime(long dateTime) {
		if (this.lastDateTime < dateTime) {
			this.lastDateTime = dateTime;
		}
	}

	/**
	 * Gets lastDateTime
	 * 
	 * @return <code>String</code>
	 * @throws ParseException
	 */
	public String getLastDateTime() {
		if (this.lastDateTime == -1L)
			return "";
		String dt = "";
		try {
	        dt = ContainerMDUtils.formatDateTime(ContainerMDUtils
	        		.longToDate(this.lastDateTime));
        } catch (ParseException e) {
        	dt = "";
        }
		return dt;
	}

	/**
	 * Returns containerMD format elements formatted into XML.
	 * 
	 */
	public void getBlockFormats(StringBuilder sb, String name, boolean bAttrNameToValue) {
		if (this.blockFormats.isEmpty())
			return;
		if (name == null) name = "blockFormat";
		ContainerMDUtils.startElement(sb, name + "s");
		toXml(sb, this.blockFormats.values(), name, bAttrNameToValue);
		ContainerMDUtils.endElement(sb, name + "s");
	}

	public void getPayloadFormats(StringBuilder sb) {
		getPayloadFormats(sb, null, false);
	}

	/**
	 * Returns containerMD format elements formatted into XML.
	 * 
	 */
	public void getPayloadFormats(StringBuilder sb, String name, boolean bAttrNameToValue) {
		if (this.payloadFormats.isEmpty())
			return;
		if (name == null) name = "payloadFormat";
		ContainerMDUtils.startElement(sb, name + "s");
		toXml(sb, this.payloadFormats.values(), name, bAttrNameToValue);
		ContainerMDUtils.endElement(sb, name + "s");
	}

	/**
	 * Returns containerMD encoding elements formatted into XML.
	 * 
	 * @return <code>String</code>
	 */
	public void getEncodings(StringBuilder sb) {
		if (this.encodings.isEmpty())
			return;
		ContainerMDUtils.startElement(sb, "encodings");
		toXml(sb, this.encodings.values());
		ContainerMDUtils.endElement(sb, "encodings");
	}

	public void getDeclaredMimeTypes(StringBuilder sb) {
		getDeclaredMimeTypes(sb, null);
	}

	/**
	 * Returns containerMD declared mimeTypes elements formatted into XML.
	 * 
	 * @return <code>String</code>
	 */
	public void getDeclaredMimeTypes(StringBuilder sb, String name)  {
		if (this.declaredMimeTypes.isEmpty())
			return;
		if (name == null) name = "declaredMimeType";
		ContainerMDUtils.startElement(sb, name + "s");
		toXml(sb, this.declaredMimeTypes.values(), name);
		ContainerMDUtils.endElement(sb, name + "s");
	}

	/**
	 * Returns containerMD host elements formatted into XML.
	 * 
	 * @return <code>String</code>
	 */
	public void getHosts(StringBuilder sb) {
		if (this.hosts.isEmpty())
			return;
		ContainerMDUtils.startElement(sb, "hosts");
		toXml(sb, this.hosts.values());
		ContainerMDUtils.endElement(sb, "hosts");
	}

	/**
	 * Returns containerMD response elements formatted into XML.
	 * 
	 * @return <code>String</code>
	 */
	public void getResponses(StringBuilder sb) {
		if (this.responses.isEmpty())
			return;
		ContainerMDUtils.startElement(sb, "responses");
		toXml(sb, this.responses.values());
		ContainerMDUtils.endElement(sb, "responses");
	}

	/**
	 * Format in XML the content of a given aggregated element
	 * 
	 * @param sb
	 *            the StringBuilder where the XML is written
	 * @param elts
	 *            the elements to format
	 */
	public void toXml(StringBuilder sb, Collection<ContainerMDElement> elts) {
		toXml(sb, elts, null, false);
	}
	public void toXml(StringBuilder sb, Collection<ContainerMDElement> elts, String name) {
		toXml(sb, elts, name, false);
	}
	public void toXml(StringBuilder sb, Collection<ContainerMDElement> elts, String name, boolean bAttrNameToValue) {
		for (ContainerMDElement e : elts) {
			e.toXml(sb, name, bAttrNameToValue);
		}
	}

	/**
	 * Handles distinct encodings
	 * 
	 * @param encoding
	 */
	public void handleEncoding(String type, String method) {
		if (!encodings.containsKey(method)) {
			ContainerMDElement container = new ContainerMDElement("encoding");

			container.getAttributes().put(ContainerMDAttributeName.TYPE, type);
			container.getAttributes().put(ContainerMDAttributeName.METHOD,
					method);
			container.getAttributes().put(ContainerMDAttributeName.ORDER,
					Integer.valueOf(encodings.size() + 1));
			encodings.put(method, container);
		}
	}

	/**
	 * Handles declared block formats
	 * 
	 * @param format
	 * @param size
	 */
	public void handleBlockFormat(String format, long size) {
		if (!ContainerMDUtils.isSet(format))
			return;

		ContainerMDElement container = blockFormats.get(format);
		if (container != null) {
			Map<ContainerMDAttributeName, Object> attrs = container
					.getAttributes();
			((AtomicInteger) attrs.get(ContainerMDAttributeName.NUMBER))
					.incrementAndGet();
			((AtomicLong) attrs.get(ContainerMDAttributeName.GLOBALSIZE))
					.addAndGet(size);
		} else {
			container = new ContainerMDElement("blockFormat");

			container.getAttributes()
					.put(ContainerMDAttributeName.NAME, format);
			container.getAttributes().put(ContainerMDAttributeName.NUMBER,
					new AtomicInteger(1));
			container.getAttributes().put(ContainerMDAttributeName.GLOBALSIZE,
					new AtomicLong(size));

			blockFormats.put(format, container);
		}
	}

	/**
	 * Handles identified formats
	 * 
	 * @param format
	 * @param size
	 */
	public void handlePayloadFormat(String format, long size) {
		if (!ContainerMDUtils.isSet(format))
			return;

		ContainerMDElement container = payloadFormats.get(format);
		if (container != null) {
			Map<ContainerMDAttributeName, Object> attrs = container
					.getAttributes();
			((AtomicInteger) attrs.get(ContainerMDAttributeName.NUMBER))
					.incrementAndGet();
			((AtomicLong) attrs.get(ContainerMDAttributeName.GLOBALSIZE))
					.addAndGet(size);
		} else {
			container = new ContainerMDElement("payloadFormat");

			container.getAttributes()
					.put(ContainerMDAttributeName.NAME, format);
			container.getAttributes().put(ContainerMDAttributeName.NUMBER,
					new AtomicInteger(1));
			container.getAttributes().put(ContainerMDAttributeName.GLOBALSIZE,
					new AtomicLong(size));

			payloadFormats.put(format, container);
		}
	}

	/**
	 * Handles distinct declared mimeTypes
	 * 
	 * @param mimeType
	 */
	public void handleDeclaredMimeType(String mimeType, long size) {
		if (!ContainerMDUtils.isSet(mimeType))
			return;

		ContainerMDElement container = declaredMimeTypes.get(mimeType);
		if (container != null) {
			Map<ContainerMDAttributeName, Object> attrs = container
					.getAttributes();
			((AtomicInteger) attrs.get(ContainerMDAttributeName.NUMBER))
					.incrementAndGet();
			((AtomicLong) attrs.get(ContainerMDAttributeName.GLOBALSIZE))
					.addAndGet(size);
		} else {
			container = new ContainerMDElement("declaredMimeType", mimeType);

			container.getAttributes().put(ContainerMDAttributeName.NUMBER,
					new AtomicInteger(1));
			container.getAttributes().put(ContainerMDAttributeName.GLOBALSIZE,
					new AtomicLong(size));
			declaredMimeTypes.put(mimeType, container);
		}
	}

	/**
	 * Handles distinct hosts
	 * 
	 * @param host
	 * @param size
	 */
	public void handleHost(String host, long size) {
		ContainerMDElement container = hosts.get(host);
		if (container != null) {
			Map<ContainerMDAttributeName, Object> attrs = container
					.getAttributes();
			((AtomicInteger) attrs.get(ContainerMDAttributeName.NUMBER))
					.incrementAndGet();
			((AtomicLong) attrs.get(ContainerMDAttributeName.GLOBALSIZE))
					.addAndGet(size);
		} else {
			container = new ContainerMDElement("host", host);
			container.getAttributes().put(ContainerMDAttributeName.NUMBER,
					new AtomicInteger(1));
			container.getAttributes().put(ContainerMDAttributeName.GLOBALSIZE,
					new AtomicLong(size));
			hosts.put(host, container);
		}
	}

	/**
	 * Handles distinct response
	 * 
	 * @param protocolVersion
	 * @param protocolName
	 * @param codeResponse
	 */
	public void handleResponse(String protocolVersion, String protocolName,
			String codeResponse, long size) {
		String key = protocolName + '|' + protocolVersion + '|' + codeResponse;
		ContainerMDElement container = responses.get(key);
		if (container != null) {
			Map<ContainerMDAttributeName, Object> attrs = container
					.getAttributes();
			((AtomicInteger) attrs.get(ContainerMDAttributeName.NUMBER))
					.incrementAndGet();
			((AtomicLong) attrs.get(ContainerMDAttributeName.GLOBALSIZE))
					.addAndGet(size);
		} else {
			container = new ContainerMDElement("response", codeResponse);
			container.getAttributes().put(ContainerMDAttributeName.NUMBER,
					new AtomicInteger(1));
			container.getAttributes().put(
					ContainerMDAttributeName.PROTOCOL_NAME, protocolName);
			container.getAttributes().put(
					ContainerMDAttributeName.PROTOCOL_VERSION, protocolVersion);
			container.getAttributes().put(ContainerMDAttributeName.GLOBALSIZE,
					new AtomicLong(size));
			responses.put(key, container);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(512);
		buf.append("ContainerMDWrapper 0x")
				.append(Integer.toHexString(System.identityHashCode(this)))
				.append(" { ");
		buf.append("minimumSize=").append(this.minimumSize).append(", ");
		buf.append("maximumSize=").append(this.maximumSize).append(", ");
		buf.append("globalSize=").append(this.globalSize).append(", ");
		buf.append("firstDateTime=").append(this.firstDateTime).append(", ");
		buf.append("lastDateTime=").append(this.lastDateTime).append(", ");
		buf.append("encodings=").append(this.encodings).append(", ");
		buf.append("blockFormats=").append(this.blockFormats).append(", ");
		buf.append("declaredMimeTypes=").append(this.declaredMimeTypes).append(", ");
		buf.append("payloadFormats=").append(this.payloadFormats).append(", ");
		buf.append("hosts=").append(this.hosts).append(", ");
		buf.append("responses=").append(this.responses);
		return buf.append(" }").toString();
	}
}
