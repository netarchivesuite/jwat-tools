package org.jwat.tools.tasks.extract;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

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
		ExtractOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static final int A_TARGET_URI = 101;

	public static ExtractOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption(null, "--queue-first", JWATTools.A_QUEUE_FIRST, 0, null);
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addOption("-u", null, A_TARGET_URI, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( ExtractTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		ExtractOptions options = new ExtractOptions();

		Argument argument;

		// Queue first.
		options.bQueueFirst = cmdLine.idMap.containsKey(JWATTools.A_QUEUE_FIRST);

		// Thread workers.
		argument = cmdLine.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				options.threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.out.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( options.threads < 1 ) {
			System.out.println( "Invalid number of threads requested: " + options.threads );
			System.exit( 1 );
		}

		argument = cmdLine.idMap.get( A_TARGET_URI );
		if ( argument != null && argument.value != null ) {
			options.targetUri = argument.value;
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
