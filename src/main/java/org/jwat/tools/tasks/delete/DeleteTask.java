package org.jwat.tools.tasks.delete;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.tasks.ProcessTask;

public class DeleteTask extends ProcessTask {

	public static final String commandName = "delete";

	public static final String commandDescription = "delete files";

	public DeleteTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools delete [-t] [-o OUTPUT_FILE] <filepattern>...");
		System.out.println("");
		System.out.println("delete one or more files");
		System.out.println("");
		System.out.println("\tDelete one or more files.");
		System.out.println("\tLinux has this nasty habit of making it hard to delete many files at the same time.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output filenames deleted");
		System.out.println(" -t        test run, do not delete files");
	}

	/** Output stream. */
	private SynchronizedOutput deletedFilesOutput;

	private boolean bTestRun;

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;

		// Output file.
		File outputFile = new File("deleted_files.out");
		argument = arguments.idMap.get( JWATTools.A_OUTPUT );
		if ( argument != null && argument.value != null ) {
			outputFile = new File(argument.value);
			if (outputFile.isDirectory()) {
				System.out.println("Can not output to a directory!");
				System.exit(1);
			} else if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
				if (!outputFile.getParentFile().mkdirs()) {
					System.out.println("Could not create parent directories!");
					System.exit(1);
				}
			}
		}

		// Test run.
		if ( arguments.idMap.containsKey( JWATTools.A_TESTRUN ) ) {
			bTestRun = true;
		}
		System.out.println("Test run: " + bTestRun);

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		deletedFilesOutput = new SynchronizedOutput(outputFile);

		threadpool_feeder_lifecycle( filesList, this );

		deletedFilesOutput.out.flush();
		deletedFilesOutput.out.close();

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
		executor.submit(new TaskRunnable(srcFile));
		queued_size += srcFile.length();
		++queued;
	}

	class TaskRunnable implements Runnable {
		File srcFile;
		TaskRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			if (!bTestRun) {
				if (!srcFile.delete()) {
					System.out.println("Could not delete file - " + srcFile.getPath());
				}
			}
			deletedFilesOutput.out.println(srcFile.getPath());
			results.add(srcFile.length());
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed deleted results list. */
	private ConcurrentLinkedQueue<Long> results = new ConcurrentLinkedQueue<Long>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			Long result;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						current_size += result;
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
