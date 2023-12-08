package org.jwat.tools.tasks.changed;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class ChangedTaskCLI extends TaskCLI {

	public static final String commandName = "changed";

	public static final String commandDescription = "changed files grouped by intervals";

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools changed <filepattern>...");
		System.out.println("");
		System.out.println("Useful command for identifying when and if files where modified in close proximity of others.");
		System.out.println("Group files by similar last modified dates.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o <file>  output intervals and files to file");
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ChangedTask task = new ChangedTask();
		ChangedOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static final int A_OUTPUT = 101;

	public static ChangedOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption("-o", null, A_OUTPUT, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( ChangedTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		ChangedOptions options = new ChangedOptions();

		Argument argument;

		// Output file.
		argument = cmdLine.idMap.get( A_OUTPUT );
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

		return options;
	}

}
