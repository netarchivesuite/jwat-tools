package org.jwat.tools.tasks.arc2warc;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
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
		Arc2WarcOptions options = new Arc2WarcOptions();

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

		// Destination directory.
		String dest = System.getProperty("user.dir");
		argument = cmdLine.idMap.get( JWATTools.A_DEST );
		if ( argument != null && argument.value != null ) {
			dest = argument.value;
		}
		System.out.println( "Using '" + dest + "' as destination directory." );
		options.destDir = new File( dest );
		if ( !options.destDir.exists() ) {
			if ( !options.destDir.mkdirs() ) {
				System.out.println( "Could not create destination directory: '" + dest + "'!" );
				System.exit( 1 );
			}
		} else if ( !options.destDir.isDirectory() ) {
			System.out.println( "'" + dest + "' is not a directory!" );
			System.exit( 1 );
		}

		// Overwrite.
		argument = cmdLine.idMap.get( JWATTools.A_OVERWRITE );
		if ( argument != null && argument.value != null ) {
			options.bOverwrite = true;
		}

		// Prefix.
		argument = cmdLine.idMap.get( JWATTools.A_PREFIX );
		if ( argument != null && argument.value != null ) {
			options.prefix = argument.value;
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		Arc2WarcTask task = new Arc2WarcTask();
		task.runtask(options);
	}

}
