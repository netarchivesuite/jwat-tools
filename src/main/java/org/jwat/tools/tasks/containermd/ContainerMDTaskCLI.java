package org.jwat.tools.tasks.containermd;

import java.io.File;

import org.jwat.common.UriProfile;
import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class ContainerMDTaskCLI extends TaskCLI {

	public static final String commandName = "containermd";

	public static final String commandDescription = "generation of containerMD for (W)ARC file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools containermd [-d outputDir] [-l] [-q] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("generate containerMD for (W)ARC files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -d <dir>        destination directory (defaults to current dir)");
		System.out.println(" -l                  relaxed URL URI validation");
		System.out.println(" -q                  quiet, no output to console");
		System.out.println(" -w<x>               set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		ContainerMDOptions options = new ContainerMDOptions();

		Argument argument;

		options.bQuiet = cmdLine.idMap.containsKey( JWATTools.A_QUIET );

		// Thread workers.
		argument = cmdLine.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				options.threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.err.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( options.threads < 1 ) {
			System.err.println( "Invalid number of threads requested: " + options.threads );
			System.exit( 1 );
		}

		// Output directory
		argument = cmdLine.idMap.get( JWATTools.A_DEST );
		if ( argument != null && argument.value != null ) {
			File dir = new File(argument.value);
			if (dir.exists()) {
				if (dir.isDirectory()) {
					options.outputDir = dir;
				} else {
					if (!options.bQuiet) System.err.println("Output '" + argument.value + "' invalid, defaulting to '" + options.outputDir + "'");
				}
			} else {
				if (dir.mkdirs()) {
					options.outputDir = dir;
				} else {
					if (!options.bQuiet) System.err.println("Output '" + argument.value + "' invalid, defaulting to '" + options.outputDir + "'");
				}
			}
		}
		
		// Relaxed URI validation.
		if ( cmdLine.idMap.containsKey( JWATTools.A_LAX ) ) {
			options.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			if (!options.bQuiet) System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}

        // Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		ContainerMDTask task = new ContainerMDTask();
		task.runtask(options);
	}

}
