package org.jwat.tools.tasks.containermd;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class ContainerMDTaskCLI extends TaskCLI {

	public static final String commandName = "containermd";

	public static final String commandDescription = "generation of containerMD for (W)ARC file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools containermd [-d outputDir] [-l] [-q] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("generate containerMD for (W)ARC files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -d <dir>  destination directory (defaults to current dir)");
		System.out.println(" -l        relaxed URL URI validation");
		System.out.println(" -q        quiet, no output to console");
		System.out.println(" -w <x>    set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ContainerMDTask task = new ContainerMDTask();
		task.runtask(ContainerMDTaskCLIParser.parseArguments(cmdLine));
	}

}
