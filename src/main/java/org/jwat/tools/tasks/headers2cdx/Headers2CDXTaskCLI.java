package org.jwat.tools.tasks.headers2cdx;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class Headers2CDXTaskCLI extends TaskCLI {

	public static final String commandName = "headers2cdx";

	public static final String commandDescription = "create a CDX index for use in wayback (unsorted)";

	@Override
	public void show_help() {
		System.out.println("jwattools headers2cdx [-o OUTPUT_FILE] [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("cdx one or more gzipped json (W)ARC/HTTP header files");
		System.out.println("");
		System.out.println("\tRead through gzipped json (W)ARC/HTTP header file(s) and create a CDX file.");
		System.out.println("\tCDX files are primarily used with Wayback.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o <file>         output cdx filename (unsorted)");
		System.out.println("    --queue-first  queue files before processing");
		System.out.println(" -w <x>            set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		Headers2CDXTask task = new Headers2CDXTask();
		task.runtask(Headers2CDXTaskCLIParser.parseArguments(cmdLine));
	}

}
