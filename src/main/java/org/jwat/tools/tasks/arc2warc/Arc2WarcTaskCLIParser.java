package org.jwat.tools.tasks.arc2warc;

import java.io.File;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class Arc2WarcTaskCLIParser {

	public static final int A_DEST = 101;
	public static final int A_OVERWRITE = 102;
	public static final int A_PREFIX = 103;

	protected Arc2WarcTaskCLIParser() {
	}

	public static Arc2WarcOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addOption("-d", "--destdir", A_DEST, 0, null).setValueRequired();
			cliOptions.addOption(null, "--overwrite", A_OVERWRITE, 0, null);
			cliOptions.addOption(null, "--prefix", A_PREFIX, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( Arc2WarcTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

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
		argument = cmdLine.idMap.get( A_DEST );
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
		if ( cmdLine.idMap.containsKey( A_OVERWRITE) ) {
			options.bOverwrite = true;
		}

		// Prefix.
		argument = cmdLine.idMap.get( A_PREFIX );
		if ( argument != null && argument.value != null ) {
			options.prefix = argument.value;
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
