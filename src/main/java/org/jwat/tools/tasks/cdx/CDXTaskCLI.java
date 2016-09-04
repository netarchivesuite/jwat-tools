package org.jwat.tools.tasks.cdx;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
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
		System.out.println(" -o<file>  output cdx filename (unsorted)");
		System.out.println(" -w<x>     set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		CDXOptions options = new CDXOptions();

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

		// Output file.
		argument = cmdLine.idMap.get( JWATTools.A_OUTPUT );
		if ( argument != null && argument.value != null ) {
			options.outputFile = new File(argument.value);
			if (options.outputFile.isDirectory()) {
				System.out.println("Can not output to a directory!");
				System.exit(1);
			} else if (options.outputFile.getParentFile() != null && !options.outputFile.getParentFile().exists()) {
				if (!options.outputFile.getParentFile().mkdirs()) {
					System.out.println("Could not create parent directories!");
					System.exit(1);
				}
			}
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		CDXTask task = new CDXTask();
		task.runtask(options);
	}

}
