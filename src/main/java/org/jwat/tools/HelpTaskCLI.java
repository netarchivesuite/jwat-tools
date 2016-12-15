package org.jwat.tools;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class HelpTaskCLI extends TaskCLI {

	public static final String commandName = "help";

	public static final String commandDescription = "display help information";

	public HelpTaskCLI() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools help [<command>]");
		System.out.println("");
		System.out.println("display help information");
		System.out.println("");
		System.out.println("\tIf no command is supplied overall help information is shown.");
		System.out.println("\tIf a command is supplied its help information is shown instead.");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		HelpOptions options = HelpTaskCLIParser.parseArguments(cmdLine);
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
