package org.jwat.tools.tasks.interval;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class IntervalTaskCLI extends TaskCLI {

	public static final String commandName = "interval";

	public static final String commandDescription = "interval extract";

	@Override
	public void show_help() {
		System.out.println("jwattools [-o<file>] interval offset1 offset2 srcfile dstfile");
		System.out.println("");
		System.out.println("extract the byte interval from offset1 to offset2 from a file");
		System.out.println("");
		System.out.println("\tSkips data up to offset1 and save data to file until offset2 is reached.");
		System.out.println("\tOffset1/2 can be decimal or hexadecimal ($<x> or 0x<x>).");
		System.out.println("\tOffset2 can also be a length-ofsset (+<x>).");
		/*
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		*/
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		IntervalTask task = new IntervalTask();
		task.runtask(IntervalTaskCLIParser.parseArguments(cmdLine));
	}

}
