package org.jwat.tools.validators;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jwat.tools.core.Validator;
import org.jwat.tools.core.ValidatorPlugin;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Thread safe XmlValidator dispenser.
 *
 * @author nicl
 */
public class XmlValidatorPlugin implements ValidatorPlugin {

	/** Basic <code>DateFormat</code> is not thread safe. */
    private static final ThreadLocal<Validator> XmlValidatorTL =
        new ThreadLocal<Validator>() {
        @Override
        public Validator initialValue() {
            return new XmlValidator();
        }
    };

    /**
     * Construct an <code>XmlValidatorPlugin</object> dispenser.
     */
    public XmlValidatorPlugin() {
    }

    @Override
	public Validator getValidator() {
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
    	 * Parse an XML document without validating DTD/XSD.
    	 * @param in XML input stream
    	 */
        public void parse(InputStream in) {
        	try {
        		builderParsing.reset();
        		builderParsing.setErrorHandler(errorHandler);
        		builderParsing.parse(in);
        	}
        	catch (IllegalArgumentException e) {
    			e.printStackTrace();
        	}
        	catch (SAXException e) {
    			e.printStackTrace();
    		}
        	catch (IOException e) {
    			e.printStackTrace();
    		}
        }

        /**
         * Parse an XML document and validate using DTD/XSD.
    	 * @param in XML input stream
         */
        public void validate(InputStream in) {
        	try {
        		builderValidating.reset();
        		builderValidating.setEntityResolver(entityResolver);
        		builderValidating.setErrorHandler(errorHandler);
        		builderValidating.parse(in);
        	}
        	catch (IllegalArgumentException e) {
    			e.printStackTrace();
        	}
        	catch (SAXException e) {
    			e.printStackTrace();
    		}
        	catch (IOException e) {
    			e.printStackTrace();
    		}
        }

    }

}
