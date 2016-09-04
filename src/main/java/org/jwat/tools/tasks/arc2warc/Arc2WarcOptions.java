package org.jwat.tools.tasks.arc2warc;

import java.io.File;
import java.util.List;

public class Arc2WarcOptions {

	public static final String DEFAULT_PREFIX = "converted-";

	public int threads = 1;

	public File destDir;

	public String prefix = DEFAULT_PREFIX;

	public boolean bOverwrite = false;

	public List<String> filesList;

}
