package org.jwat.tools.tasks.delete;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.tools.tasks.ProcessTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class DeleteTask extends ProcessTask {

	private DeleteOptions options;

	/** Output stream. */
	private SynchronizedOutput deletedFilesOutput;

	public DeleteTask() {
	}

	public void runtask(DeleteOptions options) {
		this.options = options;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		deletedFilesOutput = new SynchronizedOutput(options.outputFile);

		// FIXME Use other feeder.
		threadpool_feeder_lifecycle( options.filesList, this, 1 );

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
			if (!options.bDryRun) {
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
