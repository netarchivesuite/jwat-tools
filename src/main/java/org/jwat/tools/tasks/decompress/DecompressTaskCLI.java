package org.jwat.tools.tasks.decompress;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class DecompressTaskCLI extends TaskCLI {

	public static final String commandName = "decompress";

	public static final String commandDescription = "decompress ARC/WARC or normal GZip file(s)";

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools decompress [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("Decompress one or more GZip files.");
		System.out.println("ARC/WARC files are compressed on a record level.");
		System.out.println("Normal files are decompressed into one or more files.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println("    --queue-first  queue files before processing");
		System.out.println(" -w <x>             set the amount of worker thread(s) (defaults to 1)");
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DecompressTask task = new DecompressTask();
		DecompressOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static DecompressOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption(null, "--queue-first", JWATTools.A_QUEUE_FIRST, 0, null);
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( DecompressTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		DecompressOptions options = new DecompressOptions();

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

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
