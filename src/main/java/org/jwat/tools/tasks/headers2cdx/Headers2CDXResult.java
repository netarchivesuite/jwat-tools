package org.jwat.tools.tasks.headers2cdx;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jwat.tools.tasks.cdx.CDXEntry;

public class Headers2CDXResult {

	protected File srcFile;

	protected String filename;

	protected boolean bCompleted = false;

	protected Throwable t;

	protected List<CDXEntry> entries = new LinkedList<CDXEntry>();

}
