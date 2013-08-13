package org.jwat.tools.tasks.extract;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Arguments;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.tasks.ProcessTask;

public class ExtractTask extends ProcessTask {

	public static final String commandName = "extract";

	public static final String commandDescription = "extract ARC/WARC record(s)";

	public ExtractTask() {
	}

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

	protected String targetUri;

	@Override
	public void command(Arguments arguments) {
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

		argument = arguments.idMap.get( JWATTools.A_TARGET_URI );
		if ( argument != null && argument.value != null ) {
			targetUri = argument.value;
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
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
			case FileIdent.FILEID_GZIP:
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC_GZ:
				executor.submit(new TaskRunnable(srcFile));
				queued_size += srcFile.length();
				++queued;
				break;
			default:
				break;
			}
		} else {
			switch (fileIdent.filenameId) {
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
			case FileIdent.FILEID_GZIP:
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC_GZ:
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
			ExtractFile extractFile = new ExtractFile();
			extractFile.processFile(srcFile, targetUri);
			extractFile.srcFile = srcFile;
			results.add(extractFile);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed ExtractFile results list. */
	private ConcurrentLinkedQueue<ExtractFile> results = new ConcurrentLinkedQueue<ExtractFile>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			ExtractFile result;
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
