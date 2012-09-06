package org.jwat.tools.validators;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jwat.tools.core.Validator;
import org.jwat.tools.core.ValidatorPlugin;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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

    public XmlValidatorPlugin() {
    }

    @Override
	public Validator getValidator() {
        return XmlValidatorTL.get();
	}

    public static class XmlValidator implements Validator {

    	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 

    	private DocumentBuilderFactory factory;

    	private DocumentBuilder builder;

        public XmlValidator() {
        	factory = DocumentBuilderFactory.newInstance();
        	factory.setNamespaceAware(true);
        	factory.setValidating(true);
    		factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
    		try {
        		builder = factory.newDocumentBuilder();
    		}
    		catch (ParserConfigurationException e) {
    			throw new IllegalStateException("Could not create a new 'DocumentBuilder'!");
    		}
        }

        public void validate(InputStream in) {
        	EntityResolver entityResolver = new MyEntityResolver();
        	ErrorHandler errorHandler = new MyErrorHandler();
        	try {
        		builder.reset();
        		builder.setEntityResolver(entityResolver);
        		builder.setErrorHandler(errorHandler);
        		builder.parse(in);
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

    public static class MyEntityResolver implements EntityResolver {
		@Override
		public InputSource resolveEntity(String arg0, String arg1)
				throws SAXException, IOException {
			System.out.println(arg0);
			System.out.println(arg1);
			return null;
		}
    }

    public static class MyErrorHandler implements ErrorHandler {
		@Override
		public void fatalError(SAXParseException arg0) throws SAXException {
			System.out.println("Line " + arg0.getLineNumber() + ", Column " + arg0.getColumnNumber() + ": " + arg0.getMessage());
		}
		@Override
		public void error(SAXParseException arg0) throws SAXException {
			System.out.println("Line " + arg0.getLineNumber() + ", Column" + arg0.getColumnNumber() + ": " + arg0.getMessage());
		}
		@Override
		public void warning(SAXParseException arg0) throws SAXException {
			System.out.println("Line " + arg0.getLineNumber() + ", Column" + arg0.getColumnNumber() + ": " + arg0.getMessage());
		}
    }

}
