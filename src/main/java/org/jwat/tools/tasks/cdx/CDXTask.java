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
import org.jwat.tools.tasks.ProcessTask;

public class CDXTask extends ProcessTask {

	public static final String commandName = "cdx";

	public static final String commandDescription = "create a CDX index for use in wayback (unsorted)";

	/** Valid results output stream. */
	private SynchronizedOutput cdxOutput;

	public CDXTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools cdx [-o OUTPUT_FILE] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("cdx one or more ARC/WARC files");
		System.out.println("");
		System.out.println("\tRead through ARC/WARC file(s) and create a CDX file.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output cdx filename (unsorted)");
		System.out.println(" -w<x>     set the amount of worker thread(s) (defaults to 1)");
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

		// Output file.
		File outputFile = new File("cdx.unsorted.out");
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

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		System.out.println("Using output: " + outputFile.getPath());
		cdxOutput = new SynchronizedOutput(outputFile);
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
		cdxOutput.close();

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
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC:
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
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC:
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
			CDXFile cdxFile = new CDXFile();
			cdxFile.processFile(srcFile);
			cdxFile.srcFile = srcFile;
			results.add(cdxFile);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<CDXFile> results = new ConcurrentLinkedQueue<CDXFile>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			cdxOutput.acquire();
			cdxOutput.out.println(" CDX N b a m s k r M V g");
			cdxOutput.release();

			CDXFile cdxFile;
			List<CDXEntry> entries;
			CDXEntry entry;
			String tmpLine;
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						cdxFile = results.poll();
						entries = cdxFile.entries;
						cdxOutput.acquire();
						for (int i=0; i<entries.size(); ++i) {
							entry = entries.get(i);
							try {
								tmpLine = cdxEntry(entry, "NbamskrMVg".toCharArray());
								if (tmpLine != null) {
									//cdxOutput.out.println(cdxEntry(entry, "Abams--vg".toCharArray()));
									cdxOutput.out.println(tmpLine);
								}
							} catch (Throwable t) {
								cout.println(t.toString());
							}
						}
						cdxOutput.release();
						current_size += cdxFile.srcFile.length();
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

		// NAS
		//b e a m s c v n g

		// Wayback-1.4.2
		// A b a m s - - v g
		// net-bog-klubben.dk/1000028.pdf 20050520084930 http://www.net-bog-klubben.dk/1000028.pdf application/pdf 200 - - 820 kb-pligtsystem-44761-20121107134629-00000.warc

		// CDX N b a m s k r M V g
		// filedesc:kb-pligtsystem-44761-20121107134629-00000.warc 20121107134629 filedesc:kb-pligtsystem-44761-20121107134629-00000.warc warc/warcinfo0.1.0 - - - - 0 kb-pligtsystem-44761-20121107134629-00000.warc
		// net-bog-klubben.dk/1000028.pdf 20050520084930 http://www.net-bog-klubben.dk/1000028.pdf application/pdf 200 - - - 820 kb-pligtsystem-44761-20121107134629-00000.warc

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
				case 'N':
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
							sb.append(cUrl.toString().toLowerCase());
						} else {
							sb.append(entry.url.toLowerCase());
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
				case 'V':
					sb.append(entry.offset);
					break;
				case 'n':
					sb.append(entry.length);
					break;
				case 'g':
					sb.append(entry.fileName);
					break;
				case '-':
				default:
					sb.append('-');
					break;
				}
			}
			return sb.toString();
		}
	}

}
