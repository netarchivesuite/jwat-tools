package org.jwat.tools;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class HelpTaskCLI extends TaskCLI {

	public static final String commandName = "help";

	public static final String commandDescription = "display help information";

	public HelpTaskCLI() {
	}

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools help [<command>]");
		System.out.println("");
		System.out.println("Display help information.");
		System.out.println("If no command is supplied overall help information is shown.");
		System.out.println("If a command is supplied its help information is shown instead.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println("none");
		System.out.println("");
	}

	public static final int A_HELPFOR_COMMAND = 101;

	public static HelpOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument( "helpfor_command", A_HELPFOR_COMMAND, 1, 1);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( HelpTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		HelpOptions options = new HelpOptions();
		Argument argument = cmdLine.idMap.get(A_HELPFOR_COMMAND);
		if (argument != null) {
			options.command = argument.value;
		}
		return options;
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		HelpOptions options = parseArguments(cmdLine);
		String command = options.command;
		if (command == null) {
			JWATTools.show_help();
		}
		else {
			Class<? extends TaskCLI> clazz = JWATTools.commandMap.get(command);
			if (clazz != null) {
				try {
					TaskCLI taskcli = clazz.newInstance();
					taskcli.show_help();
				}
				catch (InstantiationException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Unknown command -- " + command);
			}
		}
	}

}
