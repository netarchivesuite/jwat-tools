package org.jwat.tools.tasks;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
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
		Options cliOptions = new Options();
		cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		UnpackOptions options = new UnpackOptions();

		Argument argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		UnpackTask task = new UnpackTask();
		task.runtask(options);
	}

}
