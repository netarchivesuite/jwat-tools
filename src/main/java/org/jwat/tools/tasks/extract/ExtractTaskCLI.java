package org.jwat.tools.tasks.extract;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
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
		System.out.println(" -u<URI>  (target)uri to extract");
		System.out.println(" -w<x>    set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ExtractOptions options = new ExtractOptions();

		Argument argument;

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

		argument = cmdLine.idMap.get( JWATTools.A_TARGET_URI );
		if ( argument != null && argument.value != null ) {
			options.targetUri = argument.value;
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		ExtractTask task = new ExtractTask();
		task.runtask(options);
	}

}
