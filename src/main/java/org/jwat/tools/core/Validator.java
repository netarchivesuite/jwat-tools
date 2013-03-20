package org.jwat.tools.core;

import org.jwat.tools.tasks.test.TestFileResultItemDiagnosis;

public interface Validator {

	public void validate(ManagedPayload managedPayload, TestFileResultItemDiagnosis itemDiagnosis);

}
