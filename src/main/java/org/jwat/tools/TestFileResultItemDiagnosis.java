package org.jwat.tools;

import java.util.LinkedList;
import java.util.List;

import org.jwat.common.Diagnosis;

public class TestFileResultItemDiagnosis {

	public long offset;

	public String type;

	public List<Diagnosis> errors = new LinkedList<Diagnosis>();

	public List<Diagnosis> warnings = new LinkedList<Diagnosis>();

}
