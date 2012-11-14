package org.jwat.tools.tasks.cdx;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.arc.ArcDateParser;
import org.jwat.common.Uri;
import org.jwat.common.UriProfile;
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

		cdxOutput = new SynchronizedOutput("cdx-unsorted.out");
		//cdxOutput.out.println("CDX b e a m s c v n g");

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

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			List<CDXEntry> entries;
			CDXEntry entry;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						entries = results.poll();
						cdxOutput.acquire();
						for (int i=0; i<entries.size(); ++i) {
							entry = entries.get(i);
							cdxOutput.out.println(cdxEntry(entry, "Abams--vg".toCharArray()));
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

		/*
		cdxOutput.out.println("CDX b e a m s c v n g");
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

		// vinavisen.dk/vinavisen/website.nsf/pages/ 20050506142753 http://www.vinavisen.dk/vinavisen/website.nsf/pages/ text/html 200 - - 294494 kb-pligtsystem-44290-20121018212853-00000.warc

		//b e a m s c v n g
		//A b a m s - - v g

		public String cdxEntry(CDXEntry entry, char[] format) {
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			char c;
			Uri uri;
			String host;
			int port;
			String query;
			for (int i=0; i<format.length; ++i) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				c = format[i];
				switch (c) {
				case '-':
					sb.append('-');
					break;
				case 'b':
					if (entry.date != null) {
						sb.append(ArcDateParser.getDateFormat().format(entry.date));
					} else {
						sb.append('-');
					}
					break;
				case 'e':
					if (entry.ip != null && entry.ip.length() > 0) {
						sb.append(entry.ip);
					} else {
						sb.append('-');
					}
					break;
				case 'A':
					if (entry.url != null && entry.url.length() > 0) {
						uri = Uri.create(entry.url, UriProfile.RFC3986_ABS_16BIT_LAX);
						StringBuilder cUrl = new StringBuilder();
						if ("http".equalsIgnoreCase(uri.getScheme())) {
							host = uri.getHost();
							port = uri.getPort();
							query = uri.getRawQuery();
							if (host.startsWith("www.")) {
								host = host.substring("www.".length());
							}
							cUrl.append(host);
							if (port != -1 && port != 80) {
								cUrl.append(':');
								cUrl.append(port);
							}
							cUrl.append(uri.getRawPath());
							if (query != null) {
								cUrl.append('?');
								cUrl.append(query);
							}
							sb.append(cUrl.toString());
						} else {
							sb.append(entry.url);
						}
					} else {
						sb.append('-');
					}
					break;
				case 'a':
					if (entry.url != null && entry.url.length() > 0) {
						sb.append(entry.url);
					} else {
						sb.append('-');
					}
					break;
				case 'm':
					if (entry.mimetype != null && entry.mimetype.length() > 0) {
						sb.append(entry.mimetype);
					} else {
						sb.append('-');
					}
					break;
				case 's':
					if (entry.responseCode != null && entry.responseCode.length() > 0) {
						sb.append(entry.responseCode);
					} else {
						sb.append('-');
					}
					break;
				case 'c':
					if (entry.checksum != null && entry.checksum.length() > 0) {
						sb.append(entry.checksum);
					} else {
						sb.append('-');
					}
					break;
				case 'v':
					sb.append(entry.offset);
					break;
				case 'n':
					sb.append(entry.length);
					break;
				case 'g':
					sb.append(entry.fileName);
					break;
				}
			}
			return sb.toString();
		}
	}

}
