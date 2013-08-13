package org.jwat.tools.tasks.arc2warc;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.tasks.ProcessTask;

public class Arc2WarcTask extends ProcessTask {

	public static final String commandName = "arc2warc";

	public static final String commandDescription = "convert ARC to WARC";

	public Arc2WarcTask() {
	}

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
		System.out.println("    --prefix     destination filename prefix (default is '" + prefix + "')");
		System.out.println(" -w <x>          set the amount of worker thread(s) (defaults to 1)");
	}

	/*
	 * Settings.
	 */

	protected File destDir;

	protected boolean bOverwrite = false;

	protected String prefix = "converted-";

    /*
	 * State.
	 */

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

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

		// Destination directory.
		String dest = System.getProperty("user.dir");
		argument = arguments.idMap.get( JWATTools.A_DEST );
		if ( argument != null && argument.value != null ) {
			dest = argument.value;
		}
		System.out.println( "Using '" + dest + "' as destination directory." );
		destDir = new File( dest );
		if ( !destDir.exists() ) {
			if ( !destDir.mkdirs() ) {
				System.out.println( "Could not create destination directory: '" + dest + "'!" );
				System.exit( 1 );
			}
		} else if ( !destDir.isDirectory() ) {
			System.out.println( "'" + dest + "' is not a directory!" );
			System.exit( 1 );
		}

		// Overwrite.
		argument = arguments.idMap.get( JWATTools.A_OVERWRITE );
		if ( argument != null && argument.value != null ) {
			bOverwrite = true;
		}

		// Prefix.
		argument = arguments.idMap.get( JWATTools.A_PREFIX );
		if ( argument != null && argument.value != null ) {
			prefix = argument.value;
		}

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		exceptionsOutput = new SynchronizedOutput("e.out");

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle(filesList, this);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		calucate_runstats();

		exceptionsOutput.close();

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
			case FileIdent.FILEID_ARC_GZ:
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
			case FileIdent.FILEID_ARC_GZ:
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
			Arc2Warc arc2warc = new Arc2Warc();
			arc2warc.arc2warc(srcFile, destDir, prefix, bOverwrite);
			arc2warc.srcFile = srcFile;
			results.add(arc2warc);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed Arc2Warc results list. */
	private ConcurrentLinkedQueue<Arc2Warc> results = new ConcurrentLinkedQueue<Arc2Warc>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			Arc2Warc result;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						current_size += result.srcFile.length();
						++processed;

						exceptionsOutput.acquire();
						if (result.exceptionList.size() > 0) {
							exceptionsOutput.out.println("#");
							exceptionsOutput.out.println("# " + result.srcFile.getPath());
							exceptionsOutput.out.println("#");
							for (int i=0; i<result.exceptionList.size(); ++i) {
								result.exceptionList.get(i).printStackTrace(exceptionsOutput.out);
							}
						}
						exceptionsOutput.release();

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
