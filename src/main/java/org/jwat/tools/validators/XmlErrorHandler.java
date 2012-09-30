package org.jwat.tools.validators;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlErrorHandler implements ErrorHandler {

	@Override
	public void fatalError(SAXParseException arg0) throws SAXException {
		System.out.println("Line " + arg0.getLineNumber() + ", Column " + arg0.getColumnNumber() + ": " + arg0.getMessage());
	}

	@Override
	public void error(SAXParseException arg0) throws SAXException {
		System.out.println("Line " + arg0.getLineNumber() + ", Column " + arg0.getColumnNumber() + ": " + arg0.getMessage());
	}

	@Override
	public void warning(SAXParseException arg0) throws SAXException {
		System.out.println("Line " + arg0.getLineNumber() + ", Column " + arg0.getColumnNumber() + ": " + arg0.getMessage());
	}

}
