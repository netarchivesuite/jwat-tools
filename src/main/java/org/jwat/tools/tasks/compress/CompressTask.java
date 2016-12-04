package org.jwat.tools.tasks.compress;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.archive.FileIdent;
import org.jwat.common.Base16;
import org.jwat.tools.tasks.ProcessTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class CompressTask extends ProcessTask {

	public CompressTask() {
	}

	private CompressOptions options;

	/** Valid results output stream. */
	private SynchronizedOutput validOutput;

	/** Invalid results output stream. */
	private SynchronizedOutput invalidOutput;

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

	public void runtask(CompressOptions options) {
		this.options = options;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle( options.filesList, this, options.threads );

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		calucate_runstats();

		cout.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		cout.println( "TotalBytes: " + toSizeString(current_size));
		cout.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));
		cout.println(String.format("    Gained: %s (%.2f%%).", toSizeString(uncompressed - compressed), current_gain));
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
			case FileIdent.FILEID_UNKNOWN:
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
				executor.submit(new TaskRunnable(srcFile));
				queued_size += srcFile.length();
				++queued;
				break;
			default:
				break;
			}
			if (fileIdent.streamId != FileIdent.FILEID_GZIP && fileIdent.streamId != FileIdent.FILEID_ARC_GZ && fileIdent.streamId != FileIdent.FILEID_WARC_GZ) {
			}
		} else {
			switch (fileIdent.filenameId) {
			case FileIdent.FILEID_UNKNOWN:
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
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
			CompressFile compressFile = new CompressFile();
			CompressResult compressionResult = compressFile.compressFile(srcFile, options);
			results.add(compressionResult);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed Compressed results list. */
	private ConcurrentLinkedQueue<CompressResult> results = new ConcurrentLinkedQueue<CompressResult>();

	private long uncompressed = 0;

	private long compressed = 0;

	private double current_gain = 0.0;

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			StringBuilder sb = new StringBuilder();
			CompressResult result;
			boolean bLoop = true;
			PrintWriter lstWriter = null;
			try {
				if (options.lstFile != null) {
					lstWriter = new PrintWriter(new BufferedWriter(new FileWriter(options.lstFile)));
				}
				while (bLoop) {
					try {
						if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
							result = results.poll();
							current_size += result.srcFile.length();
							++processed;

							if (result.bCompleted) {
					        	if (result.bVerified) {
					        		if (lstWriter != null) {
						        		sb.setLength(0);
							        	sb.append(result.srcFile.getName());
							        	sb.append(",");
							        	sb.append(result.srcFile.length());
							        	sb.append(",");
							        	sb.append(Base16.encodeArray(result.md5DigestBytesOrg));
							        	sb.append(",");
							        	sb.append(result.dstFile.getName());
							        	sb.append(",");
							        	sb.append(result.dstFile.length());
							        	sb.append(",");
							        	sb.append(Base16.encodeArray(result.md5compDigestBytesVerify));
							        	//cout.println(sb.toString());
							        	lstWriter.println(sb.toString());
					        		}
						        }
								if (!options.bVerify || result.bVerified) {
									uncompressed += result.srcFile.length();
						        	compressed += result.dstFile.length();
						        	if (uncompressed > 0) {
										current_gain = (double)(uncompressed - compressed) / (double)uncompressed * 100.0;
						        	}
								}
							}

							result.dstFile.setLastModified(result.srcFile.lastModified());

							if (options.bDryrun) {
								result.dstFile.delete();
							}
							else if (options.bRemove) {
								if (result.bCompleted && result.bVerified) {
									result.srcFile.delete();
								}
							}

							calculate_progress();

					        //cout.print_progress("Queued: " + queued + " - Processed: " + processed + " - Estimated: " + new Date(ctm + etm).toString() + ".");
							cout.print_progress(String.format("Queued: %d - Processed: %d - %s - Estimated: %s (%.2f%%) - Saved: %s (%.2f%%).", queued, processed, toSizePerSecondString(current_avgbpsec), current_timestr, current_progress, toSizeString(uncompressed - compressed), current_gain));
						}
						else if (bExit && processed == queued) {
							bLoop = false;
						}
					}
					catch (InterruptedException e) {
						bLoop = false;
					}
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
				System.err.println("Fatality!");
			}
			if (lstWriter != null) {
				lstWriter.close();
			}
			bClosed = true;
		}

	}

}
