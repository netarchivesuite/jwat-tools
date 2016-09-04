package org.jwat.tools.tasks;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
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
		UnpackOptions options = new UnpackOptions();

		Argument argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		UnpackTask task = new UnpackTask();
		task.runtask(options);
	}

}
