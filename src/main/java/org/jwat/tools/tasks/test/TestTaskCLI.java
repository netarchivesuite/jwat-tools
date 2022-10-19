package org.jwat.tools.tasks.test;

import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.CommandLine;

public class TestTaskCLI extends TaskCLI {

	public static final String commandName = "test";

	public static final String commandDescription = "test validity of ARC/WARC/GZip file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools test [-beilx] [-w THREADS] [-a<yyyyMMddHHmmss>] <filepattern>...");
		System.out.println("");
		System.out.println("test one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -a <yyyyMMddHHmmss>  only test files with last-modified after <yyyyMMddHHmmss>");
		System.out.println(" -b                   tag/rename files with errors/warnings (*.bad)");
		System.out.println(" -e                   show errors");
		System.out.println(" -h                   report HTTP header errors as ARC/WARC format errors");
		System.out.println(" -i --ignore-digest   skip digest calculation and validation");
		System.out.println(" -l                   relaxed URL URI validation");
		System.out.println(" -x                   to validate text/xml payload (eg. mets)");
		System.out.println("    --queue-first     queue files before processing");
		System.out.println(" -w <x>               set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		TestTask task = new TestTask();
		task.runtask(TestTaskCLIParser.parseArguments(cmdLine));
	}

}
