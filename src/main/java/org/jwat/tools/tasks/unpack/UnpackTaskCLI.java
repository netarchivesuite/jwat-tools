package org.jwat.tools.tasks.unpack;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class UnpackTaskCLI extends TaskCLI {

	public static final String commandName = "unpack";

	public static final String commandDescription = "unpack multifile GZip";

	@Override
	public void show_help() {
		System.out.println("Work in progress...");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		UnpackTask task = new UnpackTask();
		UnpackOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static UnpackOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( UnpackTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		UnpackOptions options = new UnpackOptions();

		Argument argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
