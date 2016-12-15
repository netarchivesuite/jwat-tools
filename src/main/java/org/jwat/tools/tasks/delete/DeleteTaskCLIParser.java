package org.jwat.tools.tasks.delete;

import java.io.File;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class DeleteTaskCLIParser {

	public static final int A_OUTPUT = 101;
	public static final int A_DRYRUN = 102;

	protected DeleteTaskCLIParser() {
	}

	public static DeleteOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
		cliOptions.addOption("-o", null, A_OUTPUT, 0, null).setValueRequired();
		cliOptions.addOption(null, "--dryrun", A_DRYRUN, 0, null);
		cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( DeleteTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		DeleteOptions options = new DeleteOptions();

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

		// Test run.
		if ( cmdLine.idMap.containsKey( A_DRYRUN ) ) {
			options.bDryRun = true;
		}
		System.out.println("Test run: " + options.bDryRun);

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
