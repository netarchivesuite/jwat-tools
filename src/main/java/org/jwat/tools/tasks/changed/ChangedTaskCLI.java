package org.jwat.tools.tasks.changed;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class ChangedTaskCLI extends TaskCLI {

	public static final String commandName = "changed";

	public static final String commandDescription = "changed files grouped by intervals";

	@Override
	public void show_help() {
		System.out.println("jwattools changed <filepattern>...");
		System.out.println("");
		System.out.println("group files by similar last modified dates");
		System.out.println("");
		System.out.println("\tUseful command for identifying when and if files where modified");
		System.out.println("\tin close proximity of others.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output intervals and files to file");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ChangedTask task = new ChangedTask();
		task.runtask(ChangedTaskCLIParser.parseArguments(cmdLine));
	}

}
