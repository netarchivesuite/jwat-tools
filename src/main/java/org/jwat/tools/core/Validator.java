package org.jwat.tools.core;

import java.io.InputStream;

import org.jwat.tools.tasks.test.TestFileResultItemDiagnosis;

public interface Validator {

	public void validate(InputStream in, TestFileResultItemDiagnosis itemDiagnosis);

}
