package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jwat.tools.tasks.ResultItemThrowable;

public class CDXResult {

	protected File srcFile;

	protected String filename;

	protected long consumed = 0;

	protected List<CDXEntry> entries = new LinkedList<CDXEntry>();

	protected List<ResultItemThrowable> throwableList = new LinkedList<ResultItemThrowable>();

	/**
	 * Get a list of CDX entries for this result.
	 * @return list of CDX entries for the input file
     */
	public List<CDXEntry> getEntries() {
		return entries;
	}

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
