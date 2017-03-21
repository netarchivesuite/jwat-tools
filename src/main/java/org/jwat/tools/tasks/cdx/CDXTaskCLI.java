package org.jwat.tools.tasks.cdx;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class CDXTaskCLI extends TaskCLI {

	public static final String commandName = "cdx";

	public static final String commandDescription = "create a CDX index for use in wayback (unsorted)";

	@Override
	public void show_help() {
		System.out.println("jwattools cdx [-o OUTPUT_FILE] [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("cdx one or more ARC/WARC files");
		System.out.println("");
		System.out.println("\tRead through ARC/WARC file(s) and create a CDX file.");
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
		CDXTask task = new CDXTask();
		task.runtask(CDXTaskCLIParser.parseArguments(cmdLine));
	}

}
