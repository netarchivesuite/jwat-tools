package org.jwat.tools.tasks.containermd;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * ContainerElement
 * 
 */
public final class ContainerMDElement {
	private SortedMap<ContainerMDAttributeName, Object> attributes = new TreeMap<ContainerMDAttributeName, Object>();

	private final String elementName;

	private final String value;

	/**
	 * Creates a new ContainerElement
	 */
	public ContainerMDElement() {
		this(null, null);
	}

	public ContainerMDElement(String name) {
		this(name, null);
	}

	public ContainerMDElement(String name, String value) {
		this.elementName = name;
		this.value = value;
	}

	/**
	 * Gets attributes
	 * 
	 * @return <code>Map</code>
	 */
	public Map<ContainerMDAttributeName, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toXml(sb);
		return sb.toString();
	}
	
	public void toXml(StringBuilder sb) {
		toXml(sb, null, false);
	}

	/**
	 * Convert to XML element
	 * 
	 * @param sb
	 */
	public void toXml(StringBuilder sb, String name, boolean bAttrNameToValue) {
		ContainerMDUtils.makeElement(sb, (name == null)? this.elementName:name, this.value,
				this.attributes, bAttrNameToValue);
	}
}