package org.jwat.tools.tasks;

import com.antiaction.common.cli.CommandLine;

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
		task.runtask(UnpackTaskCLIParser.parseArguments(cmdLine));
	}

}
