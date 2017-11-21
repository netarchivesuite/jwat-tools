package org.jwat.tools.tasks.headers2cdx;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.jwat.tools.tasks.AbstractTask;
import org.jwat.tools.tasks.cdx.CDXEntry;

import com.antiaction.common.cli.SynchronizedOutput;

public class Headers2CDXTask extends AbstractTask {

	/** Valid results output stream. */
	private SynchronizedOutput cdxOutput;

	public Headers2CDXTask() {
	}

	public void runtask(Headers2CDXOptions options) {
		System.out.println("Using output: " + options.outputFile.getPath());
		try {
			cdxOutput = new SynchronizedOutput(options.outputFile, 32*1024*1024);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		//cdxOutput.out.println("CDX b e a m s c v n g");

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle(options.filesList, options.bQueueFirst, this, options.threads);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		cdxOutput.close();

		calculate_runstats();

		cout.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		cout.println( "TotalBytes: " + toSizeString(current_size));
		cout.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));
	}

	@Override
	public void process(File srcFile) {
		if (srcFile.length() > 0) {
			if (srcFile.getName().endsWith(".headers.gz")) {
				executor.submit(new TaskRunnable(srcFile));
				queued_size += srcFile.length();
				++queued;
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
			Headers2CDXFile headersFile = new Headers2CDXFile();
			Headers2CDXResult result = headersFile.processFile(srcFile);
			results.add(result);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed CDXFile results list. */
	private ConcurrentLinkedQueue<Headers2CDXResult> results = new ConcurrentLinkedQueue<Headers2CDXResult>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		protected UrlCanonicalizer canonicalizer = new AggressiveUrlCanonicalizer(); 

		@Override
		public void run() {
			cdxOutput.acquire();
			cdxOutput.out.println(" CDX N b a m s k r M V g");
			cdxOutput.release();

			// "Abams--vg".toCharArray()
			char[] cdxformat = "NbamskrMVg".toCharArray();

			Headers2CDXResult result;
			List<CDXEntry> entries;
			Iterator<CDXEntry> iter;
			CDXEntry entry;
			String tmpLine;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						entries = result.entries;
						iter = entries.iterator();
						cdxOutput.acquire();
						while (iter.hasNext()) {
							entry = iter.next();
							try {
								tmpLine = entry.toCDXLine(result.filename, canonicalizer, cdxformat);
								if (tmpLine != null) {
									cdxOutput.out.println(tmpLine);
								}
							} catch (Throwable t) {
								cout.println(t.toString());
							}
						}
						result.entries.clear();
						cdxOutput.release();
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
