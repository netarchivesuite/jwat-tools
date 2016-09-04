package org.jwat.tools.tasks.pathindex;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class PathIndexTaskCLI extends TaskCLI {

	public static final String commandName = "pathindex";

	public static final String commandDescription = "create a path index file for use in wayback (unsorted)";

	@Override
	public void show_help() {
		System.out.println("jwattools pathindex [-o OUTPUT_FILE] <filepattern>...");
		System.out.println("");
		System.out.println("create a pathindex from one or more ARC/WARC files");
		System.out.println("");
		System.out.println("\tRead through ARC/WARC file(s) and create a pathindex file.");
		System.out.println("\tPathindex files are primarily used with Wayback.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output pathindex filename (unsorted)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		PathIndexOptions options = new PathIndexOptions();
		Argument argument;

		// Output file.
		argument = cmdLine.idMap.get( JWATTools.A_OUTPUT );
		if ( argument != null && argument.value != null ) {
			options.outputFile = new File(argument.value);
			if (options.outputFile.isDirectory()) {
				System.out.println("Can not output to a directory!");
				System.exit(1);
			} else if (options.outputFile.getParentFile() != null && !options.outputFile.getParentFile().exists()) {
				if (!options.outputFile.getParentFile().mkdirs()) {
					System.out.println("Could not create parent directories!");
					System.exit(1);
				}
			}
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		PathIndexTask task = new PathIndexTask();
		task.runtask(options);
	}

}
