package org.jwat.tools.tasks.compress;

import java.io.File;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class CompressTaskCLIParser {

	public static final int A_COMPRESS = 101;
	public static final int A_BATCHMODE = 102;
	public static final int A_DRYRUN = 103;
	public static final int A_VERIFY = 104;
	public static final int A_REMOVE = 105;
	public static final int A_DEST = 106;
	public static final int A_FILELIST = 107;
	public static final int A_TWOPASS = 108;

	protected CompressTaskCLIParser() {
	}

	public static CompressOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
		cliOptions.addOption("-1", "--fast", A_COMPRESS, 1, null);
		cliOptions.addOption("-2", null, A_COMPRESS, 2, null);
		cliOptions.addOption("-3", null, A_COMPRESS, 3, null);
		cliOptions.addOption("-4", null, A_COMPRESS, 4, null);
		cliOptions.addOption("-5", null, A_COMPRESS, 5, null);
		cliOptions.addOption("-6", null, A_COMPRESS, 6, null);
		cliOptions.addOption("-7", null, A_COMPRESS, 7, null);
		cliOptions.addOption("-8", null, A_COMPRESS, 8, null);
		cliOptions.addOption("-9", "--best", A_COMPRESS, 9, null);
		cliOptions.addOption("-d", "--destdir", A_DEST, 0, null).setValueRequired();
		cliOptions.addOption(null, "--batch", A_BATCHMODE, 0, null);
		cliOptions.addOption(null, "--remove", A_REMOVE, 0, null);
		cliOptions.addOption(null, "--verify", A_VERIFY, 0, null);
		cliOptions.addOption(null, "--dryrun", A_DRYRUN, 0, null);
		cliOptions.addOption(null, "--twopass", A_TWOPASS, 0, null);
		cliOptions.addOption(null, "--listfile", A_FILELIST, 0, null).setValueRequired();
		cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( CompressTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		CompressOptions options = new CompressOptions();

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

		// Compression level.
		argument = cmdLine.idMap.get( A_COMPRESS );
		if (argument != null) {
			options.compressionLevel = argument.option.subId;
		}
		System.out.println( "Compression level: " + options.compressionLevel );

		argument = cmdLine.idMap.get( A_BATCHMODE );
		if (argument != null) {
			options.bBatch = true;
		}
		System.out.println( "Batch mode: " + options.bBatch );

		argument = cmdLine.idMap.get( A_DRYRUN );
		if (argument != null) {
			options.bDryrun = true;
		}
		System.out.println( "Dry run: " + options.bDryrun );

		argument = cmdLine.idMap.get( A_VERIFY );
		if (argument != null) {
			options.bVerify = true;
		}
		System.out.println( "Verify output: " + options.bVerify );

		argument = cmdLine.idMap.get( A_REMOVE );
		if (argument != null) {
			options.bRemove = true;
		}
		System.out.println( "Remove input: " + options.bRemove );

		argument = cmdLine.idMap.get( A_DEST );
		if (argument != null) {
			options.dstPath = new File( argument.value );
		}
		System.out.println( "Dest path: " + options.dstPath );

		argument = cmdLine.idMap.get( A_FILELIST );
		if (argument != null) {
			options.lstFile = new File( argument.value );
		}
		System.out.println( "List file: " + options.lstFile );

		argument = cmdLine.idMap.get( A_TWOPASS );
		if (argument != null) {
			options.bTwopass = true;
		}
		System.out.println( "Twopass: " + options.bTwopass );

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}
}
