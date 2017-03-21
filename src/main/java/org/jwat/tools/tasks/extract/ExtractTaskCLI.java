package org.jwat.tools.tasks.extract;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class ExtractTaskCLI extends TaskCLI {

	public static final String commandName = "extract";

	public static final String commandDescription = "extract ARC/WARC record(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools extract [-u URI] [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("extract one or more entries/records from GZip/ARC/WARC files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -u <URI>          (target)uri to extract");
		System.out.println("    --queue-first  queue files before processing");
		System.out.println(" -w <x>            set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ExtractTask task = new ExtractTask();
		task.runtask(ExtractTaskCLIParser.parseArguments(cmdLine));
	}

}
