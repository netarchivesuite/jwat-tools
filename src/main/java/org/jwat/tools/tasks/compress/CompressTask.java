package org.jwat.tools.tasks.compress;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.tasks.ProcessTask;

public class CompressTask extends ProcessTask {

	public static final String commandName = "compress";

	public static final String commandDescription = "compress ARC/WARC or plain file(s)";

	public int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	public CompressTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools compress [-123456789] [--fast] [--slow] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("compress one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("\tNormal files are compressed as a single GZip file.");
		System.out.println("\tARC/WARC files are compressed on a record level.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -1, --fast  compress faster");
		System.out.println(" -9, --slow  compress better");
		System.out.println(" -w<x>        set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		// Thread workers.
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.out.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( threads < 1 ) {
			System.out.println( "Invalid number of threads requested: " + threads );
			System.exit( 1 );
		}

		// Compression level.
		argument = arguments.idMap.get( JWATTools.A_COMPRESS );
		if (argument != null) {
			compressionLevel = argument.argDef.subId;
			System.out.println( "Compression level: " + compressionLevel );
		}

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle( filesList, this );

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		calucate_runstats();

		cout.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		cout.println( "TotalBytes: " + toSizeString(current_size));
		cout.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));
	}

	@Override
	public void process(File srcFile) {
		FileIdent fileIdent = FileIdent.ident(srcFile);
		if (srcFile.length() > 0) {
			// debug
			//System.out.println(fileIdent.filenameId + " " + fileIdent.streamId + " " + srcFile.getName());
			if (fileIdent.filenameId != fileIdent.streamId) {
				cout.println("Wrong extension: '" + srcFile.getPath() + "'");
			}
			switch (fileIdent.streamId) {
			case FileIdent.FILEID_UNKNOWN:
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
				executor.submit(new TaskRunnable(srcFile));
				queued_size += srcFile.length();
				++queued;
				break;
			default:
				break;
			}
			if (fileIdent.streamId != FileIdent.FILEID_GZIP && fileIdent.streamId != FileIdent.FILEID_ARC_GZ && fileIdent.streamId != FileIdent.FILEID_WARC_GZ) {
			}
		} else {
			switch (fileIdent.filenameId) {
			case FileIdent.FILEID_UNKNOWN:
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
				cout.println("Empty file: '" + srcFile.getPath() + "'");
				break;
			default:
				break;
			}
		}
	}

	class TaskRunnable implements Runnable {
		File srcFile;
		TaskRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			CompressFile compressFile = new CompressFile();
			compressFile.compressFile(srcFile);
			compressFile.srcFile = srcFile;
			results.add(compressFile);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<CompressFile> results = new ConcurrentLinkedQueue<CompressFile>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			CompressFile result;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						current_size += result.srcFile.length();
						++processed;

						calculate_progress();

						//cout.print_progress("Queued: " + queued + " - Processed: " + processed + " - Estimated: " + new Date(ctm + etm).toString() + ".");
						cout.print_progress(String.format("Queued: %d - Processed: %d - %s - Estimated: %s (%.2f%%).", queued, processed, toSizePerSecondString(current_avgbpsec), current_timestr, current_progress));
					} else if (bExit && processed == queued) {
						bLoop = false;
					}
				} catch (InterruptedException e) {
					bLoop = false;
				}
			}
			bClosed = true;
		}

	}

}
