package org.jwat.tools.tasks.decompress;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class DecompressTaskCLI extends TaskCLI {

	public static final String commandName = "decompress";

	public static final String commandDescription = "decompress ARC/WARC or normal GZip file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools decompress [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("decompress one or more GZip files");
		System.out.println("");
		System.out.println("\tNormal files are decompressed into one or more files.");
		System.out.println("\tARC/WARC files are compressed on a record level.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -w<x>  set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DecompressTask task = new DecompressTask();
		task.runtask(DecompressTaskCLIParser.parseArguments(cmdLine));
	}

}
