package org.jwat.tools.tasks.arc2warc;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class Arc2WarcTaskCLI extends TaskCLI {

	public static final String commandName = "arc2warc";

	public static final String commandDescription = "convert ARC to WARC";

	@Override
	public void show_help() {
		System.out.println("jwattools arc2warc [-d DIR] [--overwrite]... [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("arc2warc will convert one or more ARC file(s) to WARC file(s).");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -d <dir>        destination directory (defaults to current dir)");
		System.out.println("    --overwrite  overwrite destination file (default is to skip file)");
		System.out.println("    --prefix     destination filename prefix (default is '" + Arc2WarcOptions.DEFAULT_PREFIX + "')");
		System.out.println(" -w <x>          set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		Arc2WarcTask task = new Arc2WarcTask();
		task.runtask(Arc2WarcTaskCLIParser.parseArguments(cmdLine));
	}

}
