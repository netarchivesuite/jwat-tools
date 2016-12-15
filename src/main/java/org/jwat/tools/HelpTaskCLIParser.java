package org.jwat.tools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class HelpTaskCLIParser {

	public static final int A_HELPFOR_COMMAND = 101;

	protected HelpTaskCLIParser() {
	}

	public static HelpOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addNamedArgument( "helpfor_command", A_HELPFOR_COMMAND, 1, 1);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( HelpTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		HelpOptions options = new HelpOptions();
		Argument argument = cmdLine.idMap.get(A_HELPFOR_COMMAND);
		if (argument != null) {
			options.command = argument.value;
		}
		return options;
	}

}
