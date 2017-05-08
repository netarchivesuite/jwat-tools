package org.jwat.tools.tasks.headers2cdx;

import java.io.File;
import java.util.List;

public class Headers2CDXOptions {

	public static final String DEFAULT_CDXOUTPUT_FILENAME = "cdx.unsorted.out";

	public boolean bQueueFirst = false;

	public int threads = 1;

	public File outputFile = new File(DEFAULT_CDXOUTPUT_FILENAME);

	public List<String> filesList;

}
