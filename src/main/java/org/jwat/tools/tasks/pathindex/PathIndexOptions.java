package org.jwat.tools.tasks.pathindex;

import java.io.File;
import java.util.List;

public class PathIndexOptions {

	public static String DEFAULT_OUTPUT_FILENAME = "path-index.unsorted.out";

	public File outputFile = new File(DEFAULT_OUTPUT_FILENAME);

	public List<String> filesList;

}
