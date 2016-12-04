package org.jwat.tools.tasks.compress;

import java.io.File;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class CompressTaskCLI extends TaskCLI {

	public static final String commandName = "compress";

	public static final String commandDescription = "compress ARC/WARC or plain file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools compress [-123456789] [--fast] [--best] [-w THREADS] <filepattern>...");
		System.out.println("");
		System.out.println("compress one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("\tNormal files are compressed as a single GZip file.");
		System.out.println("\tARC/WARC files are compressed on a record level.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -1, --fast      compress faster, low compression ratio");
		System.out.println(" -9, --best      compesss better, high compression ratio");
		System.out.println(" -d, --destdir   destination directory of compressed files");
		System.out.println("     --dryrun    remove output file leaving the orignal in place");
		System.out.println("     --verify    decompress output file and compare against input file");
		System.out.println("     --remove    remove input file after compression (only on success)");
		System.out.println("     --listfile  list file of old/new filename, length and checksum");
		System.out.println("     --twopass   index file and then bitstream compress based on index");
		System.out.println(" -w<x>           set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
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
		argument = cmdLine.idMap.get( JWATTools.A_COMPRESS );
		if (argument != null) {
			options.compressionLevel = argument.option.subId;
		}
		System.out.println( "Compression level: " + options.compressionLevel );

		argument = cmdLine.idMap.get( JWATTools.A_BATCHMODE );
		if (argument != null) {
			options.bBatch = true;
		}
		System.out.println( "Batch mode: " + options.bBatch );

		argument = cmdLine.idMap.get( JWATTools.A_DRYRUN );
		if (argument != null) {
			options.bDryrun = true;
		}
		System.out.println( "Dry run: " + options.bDryrun );

		argument = cmdLine.idMap.get( JWATTools.A_VERIFY );
		if (argument != null) {
			options.bVerify = true;
		}
		System.out.println( "Verify output: " + options.bVerify );

		argument = cmdLine.idMap.get( JWATTools.A_REMOVE );
		if (argument != null) {
			options.bRemove = true;
		}
		System.out.println( "Remove input: " + options.bRemove );

		argument = cmdLine.idMap.get( JWATTools.A_DEST );
		if (argument != null) {
			options.dstPath = new File( argument.value );
		}
		System.out.println( "Dest path: " + options.dstPath );

		argument = cmdLine.idMap.get( JWATTools.A_FILELIST );
		if (argument != null) {
			options.lstFile = new File( argument.value );
		}
		System.out.println( "List file: " + options.lstFile );

		argument = cmdLine.idMap.get( JWATTools.A_TWOPASS );
		if (argument != null) {
			options.bTwopass = true;
		}
		System.out.println( "Twopass: " + options.bTwopass );

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		CompressTask task = new CompressTask();
		task.runtask(options);
	}

}
