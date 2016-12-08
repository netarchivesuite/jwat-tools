package org.jwat.tools.tasks.compress;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class CompressTaskCLI extends TaskCLI {

	public static final String commandName = "compress";

	public static final String commandDescription = "compress ARC/WARC or plain file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools compress [-123456789] [--fast] [--best] [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("compress one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("\tNormal files are compressed as a single GZip file.");
		System.out.println("\tARC/WARC files are compressed on a record level.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -1, --fast      compress faster, low compression ratio");
		System.out.println(" -9, --best      compesss better, high compression ratio");
		System.out.println(" -d, --destdir   destination directory of compressed files");
		System.out.println("     --dryrun    remove output file leaving the orignal in place");
		System.out.println("     --verify    decompress output file and compare against input file");
		System.out.println("     --remove    remove input file after compression (only on success)");
		System.out.println("     --listfile  list file of old/new filename, length and checksum");
		System.out.println("     --twopass   index file and then bitstream compress based on index");
		System.out.println(" -w<x>           set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		CompressTask task = new CompressTask();
		task.runtask(CompressTaskCLIParser.parseArguments(cmdLine));
	}

}
