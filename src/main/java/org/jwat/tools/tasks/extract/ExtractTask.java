package org.jwat.tools.tasks.extract;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.archive.FileIdent;
import org.jwat.tools.tasks.AbstractTask;

public class ExtractTask extends AbstractTask {

    private ExtractOptions options;

	/*
	 * Settings.
	 */

	private int recordHeaderMaxSize = 1024 * 1024;
    private int payloadHeaderMaxSize = 1024 * 1024;

	public ExtractTask() {
	}

	public void runtask(ExtractOptions options) {
		this.options = options;
		options.recordHeaderMaxSize = recordHeaderMaxSize;
		options.payloadHeaderMaxSize = payloadHeaderMaxSize;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle( options.filesList, options.bQueueFirst, this, options.threads );

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		calculate_runstats();

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
			ExtractResult result = extractFile.processFile(srcFile, options);
			results.add(result);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed ExtractFile results list. */
	private ConcurrentLinkedQueue<ExtractResult> results = new ConcurrentLinkedQueue<ExtractResult>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			ExtractResult result;
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
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			bClosed = true;
		}

	}

}
