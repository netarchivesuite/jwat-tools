package org.jwat.tools.tasks.extract;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class ExtractTaskCLIParser {

	public static final int A_TARGET_URI = 101;

	protected ExtractTaskCLIParser() {
	}

	public static ExtractOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addOption("-u", null, A_TARGET_URI, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( ExtractTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		ExtractOptions options = new ExtractOptions();

		Argument argument;

		// Thread workers.
		argument = cmdLine.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				options.threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.out.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( options.threads < 1 ) {
			System.out.println( "Invalid number of threads requested: " + options.threads );
			System.exit( 1 );
		}

		argument = cmdLine.idMap.get( A_TARGET_URI );
		if ( argument != null && argument.value != null ) {
			options.targetUri = argument.value;
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
