package org.jwat.tools.tasks.delete;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class DeleteTaskCLI extends TaskCLI {

	public static final String commandName = "delete";

	public static final String commandDescription = "delete files";

	@Override
	public void show_help() {
		System.out.println("jwattools delete [-t] [-o OUTPUT_FILE] <filepattern>...");
		System.out.println("");
		System.out.println("delete one or more files");
		System.out.println("");
		System.out.println("\tDelete one or more files.");
		System.out.println("\tLinux has this nasty habit of making it hard to delete many files at the same time.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output filenames deleted");
		System.out.println(" -t        test run, do not delete files");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DeleteOptions options = new DeleteOptions();

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

		// Test run.
		if ( cmdLine.idMap.containsKey( JWATTools.A_TESTRUN ) ) {
			options.bTestRun = true;
		}
		System.out.println("Test run: " + options.bTestRun);

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		DeleteTask task = new DeleteTask();
		task.runtask(options);
	}

}
