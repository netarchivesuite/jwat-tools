package org.jwat.tools.tasks.delete;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class DeleteTaskCLI extends TaskCLI {

	public static final String commandName = "delete";

	public static final String commandDescription = "delete files";

	@Override
	public void show_help() {
		System.out.println("jwattools delete [-t] [-o OUTPUT_FILE] <filepattern>...");
		System.out.println("");
		System.out.println("delete one or more files");
		System.out.println("");
		System.out.println("\tDelete one or more files.");
		System.out.println("\tLinux has this nasty habit of making it hard to delete many files at the same time.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o <file>    output filenames deleted");
		System.out.println("    --dryrun  dry run, does not delete files");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DeleteTask task = new DeleteTask();
		task.runtask(DeleteTaskCLIParser.parseArguments(cmdLine));
	}

}
