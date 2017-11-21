package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.util.List;

public class CDXOptions {

	public static final String DEFAULT_CDXOUTPUT_FILENAME = "cdx.unsorted.out";

	public int threads = 1;

	public boolean bQueueFirst = false;

	public boolean bValidateDigest = false;

	public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;

	public File outputFile = new File(DEFAULT_CDXOUTPUT_FILENAME);

	public List<String> filesList;

}
