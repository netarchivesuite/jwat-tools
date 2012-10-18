package org.jwat.tools.validators;

import org.jwat.common.Diagnosis;
import org.jwat.common.DiagnosisType;
import org.jwat.tools.tasks.test.TestFileResultItemDiagnosis;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlErrorHandler implements ErrorHandler {

	protected TestFileResultItemDiagnosis itemDiagnosis;

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		if (itemDiagnosis != null) {
			Diagnosis diagnosis = new Diagnosis(DiagnosisType.ERROR, "XML document",
					"Line " + exception.getLineNumber(),
					"Column " + exception.getColumnNumber(),
					exception.getMessage());
			itemDiagnosis.errors.add(diagnosis);
		} else {
			System.out.println("Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage());
		}
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		if (itemDiagnosis != null) {
			Diagnosis diagnosis = new Diagnosis(DiagnosisType.ERROR, "XML document",
					"Line " + exception.getLineNumber(),
					"Column " + exception.getColumnNumber(),
					exception.getMessage());
			itemDiagnosis.errors.add(diagnosis);
		} else {
			System.out.println("Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage());
		}
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		if (itemDiagnosis != null) {
			Diagnosis diagnosis = new Diagnosis(DiagnosisType.ERROR, "XML document",
					"Line " + exception.getLineNumber(),
					"Column " + exception.getColumnNumber(),
					exception.getMessage());
			itemDiagnosis.warnings.add(diagnosis);
		} else {
			System.out.println("Line " + exception.getLineNumber() + ", Column " + exception.getColumnNumber() + ": " + exception.getMessage());
		}
	}

}
