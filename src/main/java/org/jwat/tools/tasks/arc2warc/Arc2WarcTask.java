package org.jwat.tools.tasks.arc2warc;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.archive.FileIdent;
import org.jwat.tools.tasks.ProcessTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class Arc2WarcTask extends ProcessTask {

	private Arc2WarcOptions options;

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

	public Arc2WarcTask() {
	}

	/*
	 * Settings.
	 */

	public void runtask(Arc2WarcOptions options) {
		this.options = options;

		exceptionsOutput = new SynchronizedOutput("e.out");

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle(options.filesList, this, options.threads);

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
			arc2warc.arc2warc(srcFile, options);
			// FIXME
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
