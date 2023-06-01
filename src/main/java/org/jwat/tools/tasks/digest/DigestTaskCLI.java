package org.jwat.tools.tasks.digest;

import java.util.LinkedList;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class DigestTaskCLI extends TaskCLI {

	public static final String commandName = "digest";

	public static final String commandDescription = "digest calculation";

	@Override
	public void show_help() {
		System.out.println("jwattools [-o<file>] digest ");
		System.out.println("");
		System.out.println("digest file(s)");
		System.out.println("");
		System.out.println("\tDigest file(s).");
		/*
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		*/
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DigestTask task = new DigestTask();
		DigestOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static DigestOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument( "files", JWATTools.A_FILES, 1, 1 );
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( DigestTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		DigestOptions options = new DigestOptions();

		Argument argument;
		String tmpStr;

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = new LinkedList<String>();
		//options.filesList = argument.values;
		options.filesList.add( argument.value );

		return options;
	}

}
