package org.jwat.tools.tasks;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class UnpackTaskCLIParser {

	protected UnpackTaskCLIParser() {
	}

	public static UnpackOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( UnpackTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		UnpackOptions options = new UnpackOptions();

		Argument argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
