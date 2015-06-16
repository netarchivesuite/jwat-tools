package org.jwat.tools.validators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jwat.archive.ManagedPayload;
import org.jwat.tools.core.Validator;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.tools.tasks.test.TestFileResultItemDiagnosis;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;

/**
 * Thread safe XmlValidator dispenser.
 *
 * @author nicl
 */
public class XmlValidatorPlugin implements ValidatorPlugin {

	/** Basic <code>DateFormat</code> is not thread safe. */
    private static final ThreadLocal<XmlValidator> XmlValidatorTL =
        new ThreadLocal<XmlValidator>() {
        @Override
        public XmlValidator initialValue() {
            return new XmlValidator();
        }
    };

    /**
     * Construct an <code>XmlValidatorPlugin</object> dispenser.
     */
    public XmlValidatorPlugin() {
    }

    @Override
	public XmlValidator getValidator() {
        return XmlValidatorTL.get();
	}

    public static class XmlValidator implements Validator {

    	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 

    	private static EntityResolver entityResolver;

    	static {
    		String jwattools_home = System.getenv("JWATTOOLS_HOME");
    		File home = new File(jwattools_home, "cache");
    		entityResolver = new XmlEntityResolver(home);
    	}

    	private DocumentBuilderFactory factoryParsing;
    	private DocumentBuilder builderParsing;

    	private DocumentBuilderFactory factoryValidating;
    	private DocumentBuilder builderValidating;

    	/** <code>XmlErrorHandler</code> used to report errors and/or warnings. */
    	private XmlErrorHandler errorHandler;

    	public Document document = null;

		public String systemId = null; 

		/**
    	 * Construct an <code>XmlValidator</code>.
    	 */
    	public XmlValidator() {
        	factoryParsing = DocumentBuilderFactory.newInstance();
        	factoryParsing.setNamespaceAware(true);
        	factoryParsing.setValidating(false);
    		factoryValidating = DocumentBuilderFactory.newInstance();
        	factoryValidating.setNamespaceAware(true);
        	factoryValidating.setValidating(true);
    		factoryValidating.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
    		try {
        		builderParsing = factoryParsing.newDocumentBuilder();
        		builderValidating = factoryValidating.newDocumentBuilder();
    		}
    		catch (ParserConfigurationException e) {
    			throw new IllegalStateException("Could not create a new 'DocumentBuilder'!");
    		}
    		errorHandler = new XmlErrorHandler();
        }

        /**
    	 * Load and validate an XML document in two passes.
    	 * First pass load the XML document for wellformedness.
    	 * Second pass looks for DTD/XSD and validate against it.
    	 * @param in XML input stream
         */
        public void validate(ManagedPayload managedPayload, TestFileResultItemDiagnosis itemDiagnosis) {
        	InputStream payloadStream = null;
        	document = null;
        	try {
        		payloadStream = managedPayload.getPayloadStream();
        		errorHandler.itemDiagnosis = itemDiagnosis;
        		builderParsing.reset();
        		builderParsing.setErrorHandler(errorHandler);
        		document = builderParsing.parse(payloadStream);
        		payloadStream.close();
        		payloadStream = null;
        		systemId = null;
        		DocumentType documentType = document.getDoctype();
        		boolean bValidate = false;
        		if (documentType != null) {
            		systemId = documentType.getSystemId();
        		}
        		if (systemId != null) {
        			// debug
        			//System.out.println("systemId: " + systemId);
            		bValidate = true;
        		}
        		else {
            		XPathFactory xpf = XPathFactory.newInstance();
            		XPath xp = xpf.newXPath();
            		NodeList nodes;
        			Node node;
        			// JDK6 XPath engine supposedly only implements v1.0 of the specs.
            		nodes = (NodeList)xp.evaluate("//*", document.getDocumentElement(), XPathConstants.NODESET);
            		for (int i = 0; i < nodes.getLength(); i++) {
            			node = nodes.item(i).getAttributes().getNamedItem("xmlns:xsi");
            			if (node != null) {
            				// debug
            				//System.out.println(node.getNodeValue());
                    		bValidate = true;
            			}
            			node = nodes.item(i).getAttributes().getNamedItem("xsi:schemaLocation");
            			if (node != null) {
            				// debug
            				//System.out.println(node.getNodeValue());
                    		bValidate = true;
            			}
            		}
        		}
        		if (bValidate) {
            		payloadStream = managedPayload.getPayloadStream();
            		errorHandler.itemDiagnosis = itemDiagnosis;
            		builderValidating.reset();
            		builderValidating.setEntityResolver(entityResolver);
            		builderValidating.setErrorHandler(errorHandler);
            		document = builderValidating.parse(payloadStream);
            		payloadStream.close();
            		payloadStream = null;
        		}
        	}
        	catch (Throwable t) {
        		if (itemDiagnosis != null) {
        			itemDiagnosis.throwables.add(t);
        		}
        		else {
        			t.printStackTrace();
        		}
        	}
        	finally {
        		if (payloadStream != null) {
        			try {
						payloadStream.close();
					} catch (IOException e) {
		        		if (itemDiagnosis != null) {
		        			itemDiagnosis.throwables.add(e);
		        		}
		        		else {
		        			e.printStackTrace();
		        		}
					}
        			payloadStream = null;
        		}
        	}
        }

    }

}
