package org.jwat.tools.tasks.delete;

import java.io.File;
import java.util.List;

public class DeleteOptions {

	public static String DEFAULT_DELETEDFILES_FILENAME = "deleted_files.out";

	public File outputFile = new File(DEFAULT_DELETEDFILES_FILENAME);

	public boolean bDryRun;

	public List<String> filesList;

}
