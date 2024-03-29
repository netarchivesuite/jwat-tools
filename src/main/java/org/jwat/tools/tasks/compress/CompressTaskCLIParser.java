package org.jwat.tools.tasks.compress;

import java.io.File;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
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
	public static final int A_HDRFILES = 109;
	public static final int A_BLACKLIST = 110;
	public static final int A_CHECKSUMS = 111;

	protected CompressTaskCLIParser() {
	}

	public static CompressOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption(null, "--queue-first", JWATTools.A_QUEUE_FIRST, 0, null);
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
			cliOptions.addOption(null, "--hdrfiles", A_HDRFILES, 0, null);
			cliOptions.addOption("-q", "--quiet", JWATTools.A_QUIET, 0, null);
			cliOptions.addOption(null, "--blacklist", A_BLACKLIST, 0, null).setValueRequired();
			cliOptions.addOption(null, "--checksums", A_CHECKSUMS, 0, null).setValueRequired();
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( CompressTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		CompressOptions options = new CompressOptions();

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

		// Compression level.
		argument = cmdLine.idMap.get( A_COMPRESS );
		if (argument != null) {
			options.compressionLevel = argument.option.subId;
		}

		argument = cmdLine.idMap.get( A_BATCHMODE );
		if (argument != null) {
			options.bBatch = true;
		}

		argument = cmdLine.idMap.get( A_DRYRUN );
		if (argument != null) {
			options.bDryrun = true;
		}

		argument = cmdLine.idMap.get( A_VERIFY );
		if (argument != null) {
			options.bVerify = true;
		}

		argument = cmdLine.idMap.get( A_REMOVE );
		if (argument != null) {
			options.bRemove = true;
		}

		argument = cmdLine.idMap.get( A_DEST );
		if (argument != null) {
			options.dstPath = new File( argument.value );
		}

		argument = cmdLine.idMap.get( A_FILELIST );
		if (argument != null) {
			options.lstFile = new File( argument.value );
		}

		argument = cmdLine.idMap.get( A_TWOPASS );
		if (argument != null) {
			options.bTwopass = true;
		}

		argument = cmdLine.idMap.get( A_HDRFILES );
		if (argument != null) {
			options.bHeaderFiles = true;
		}

		argument = cmdLine.idMap.get( A_BLACKLIST );
		if (argument != null) {
			options.blacklistFile = new File( argument.value );
		}

		argument = cmdLine.idMap.get( A_CHECKSUMS );
		if (argument != null) {
			options.checksumsFile = new File( argument.value );
		}

		options.bQuiet = cmdLine.idMap.containsKey( JWATTools.A_QUIET );

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		if (!options.bQuiet) {
			System.out.println("JWATTools v" + JWATTools.getVersionString("org.jwat.tools"));
			System.out.println( "Compression level: " + options.compressionLevel );
			System.out.println( "       Batch mode: " + options.bBatch );
			System.out.println( "          Dry run: " + options.bDryrun );
			System.out.println( "    Verify output: " + options.bVerify );
			System.out.println( "     Remove input: " + options.bRemove );
			System.out.println( "        Dest path: " + options.dstPath );
			System.out.println( "        List file: " + options.lstFile );
			System.out.println( "          Twopass: " + options.bTwopass );
			System.out.println( "     Header Files: " + options.bHeaderFiles );
			System.out.println( "            Quiet: " + options.bQuiet );
		}

		return options;
	}

}
