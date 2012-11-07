package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.arc.ArcDateParser;
import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.core.Task;

public class CDXTask extends Task {

	/** Valid results output stream. */
	private SynchronizedOutput cdxOutput;

	public CDXTask() {
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		cdxOutput = new SynchronizedOutput("cdx.out");
		cdxOutput.out.println("CDX b e a m s c v n g");

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle(filesList);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void process(File srcFile) {
		if (srcFile.length() > 0) {
			int fileId = FileIdent.identFile(srcFile);
			if (fileId > 0) {
				executor.submit(new TaskRunnable(srcFile));
				++queued;
			} else {
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
			CDXFile cdxFile = new CDXFile();
			//testFile.callback = null;
			List<CDXEntry> entries = cdxFile.processFile(srcFile);
			results.add(entries);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<List<CDXEntry>> results = new ConcurrentLinkedQueue<List<CDXEntry>>();

	/*
	date
	ip
	url
	mimetype
	response code
	old stylechecksum
	v uncompressed arc file offset * 
	n arc document length * 
	g file name 
	*/

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			List<CDXEntry> entries;
			CDXEntry entry;
			StringBuilder sb = new StringBuilder();
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						entries = results.poll();
						cdxOutput.acquire();
						for (int i=0; i<entries.size(); ++i) {
							entry = entries.get(i);
							sb.setLength(0);
							if (entry.date != null) {
								sb.append(ArcDateParser.getDateFormat().format(entry.date));
							} else {
								sb.append('-');
							}
							sb.append(' ');
							if (entry.ip != null && entry.ip.length() > 0) {
								sb.append(entry.ip);
							} else {
								sb.append('-');
							}
							sb.append(' ');
							if (entry.url != null && entry.url.length() > 0) {
								sb.append(entry.url);
							} else {
								sb.append('-');
							}
							sb.append(' ');
							if (entry.mimetype != null && entry.mimetype.length() > 0) {
								sb.append(entry.mimetype);
							} else {
								sb.append('-');
							}
							sb.append(' ');
							if (entry.responseCode != null && entry.responseCode.length() > 0) {
								sb.append(entry.responseCode);
							} else {
								sb.append('-');
							}
							sb.append(' ');
							if (entry.checksum != null && entry.checksum.length() > 0) {
								sb.append(entry.checksum);
							} else {
								sb.append('-');
							}
							sb.append(' ');
							sb.append(entry.offset); 
							sb.append(' ');
							sb.append(entry.length); 
							sb.append(' ');
							sb.append(entry.fileName); 
							cdxOutput.out.println(sb.toString());
						}
						cdxOutput.release();
						++processed;
						cout.print_progress("Queued: " + queued + " - Processed: " + processed + ".");
					} else if (bExit) {
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