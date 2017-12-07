package org.jwat.tools.tasks.extract;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jwat.tools.tasks.ResultItemThrowable;

public class ExtractResult {

    protected File srcFile;

	protected String fileName;

	protected int recordNr = 1;

	protected long consumed = 0;

	protected List<ResultItemThrowable> throwableList = new LinkedList<ResultItemThrowable>();

	/**
	 * Get a list of throwables encountered during the input processing.
	 * @return list of throwables encountered during input file processing
	 */
	public List<ResultItemThrowable> getThrowables() {
		return throwableList;
	}

	/**
	 * Returns true if any throwables were encountered during the input processing, false otherwise.
	 * @return boolean value indicating whether any throwables were encountered during input file processing
	 */
	public boolean hasFailed() {
		return !throwableList.isEmpty();
	}

}
