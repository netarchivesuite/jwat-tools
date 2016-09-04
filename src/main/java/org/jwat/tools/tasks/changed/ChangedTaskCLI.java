package org.jwat.tools.tasks.changed;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class ChangedTaskCLI extends TaskCLI {

	public static final String commandName = "changed";

	public static final String commandDescription = "changed files grouped by intervals";

	@Override
	public void show_help() {
		System.out.println("jwattools changed <filepattern>...");
		System.out.println("");
		System.out.println("group files by similar last modified dates");
		System.out.println("");
		System.out.println("\tUseful command for identifying when and if files where modified");
		System.out.println("\tin close proximity of others.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output intervals and files to file");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ChangedOptions options = new ChangedOptions();

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

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		ChangedTask task = new ChangedTask();
		task.runtask(options);
	}

}
