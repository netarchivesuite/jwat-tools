package org.jwat.tools.tasks.compress;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.archive.FileIdent;
import org.jwat.common.Base16;
import org.jwat.tools.tasks.AbstractTask;

import com.antiaction.common.cli.SynchronizedOutput;
import com.antiaction.common.datastructures.flatfilelookup.FlatfileLookupAbstract;
import com.antiaction.common.datastructures.flatfilelookup.FlatfileLookupCaching;
import com.antiaction.common.datastructures.flatfilelookup.PrefixStringComparator;

public class CompressTask extends AbstractTask {

	public CompressTask() {
	}

	private CompressOptions options;

	/*
	 * Settings.
	 */

	private int recordHeaderMaxSize = 1024 * 1024;
    private int payloadHeaderMaxSize = 1024 * 1024;

    private Set<String> blacklistMap;

    private int blacklisted = 0;

	private FlatfileLookupAbstract ffl;

	private RandomAccessFile checksumsRaf;

	private int missingChecksum = 0;

	private int wrongChecksum = 0;

	private PrefixStringComparator psComparator = new PrefixStringComparator();

	/** Integrity fails  output stream. */
	private SynchronizedOutput failsOutput;

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

    /** Cmopress report output stream. */
	private SynchronizedOutput compressReportOutput;

	public void runtask(CompressOptions options) {
		this.options = options;
	    options.recordHeaderMaxSize = recordHeaderMaxSize;
	    options.payloadHeaderMaxSize = payloadHeaderMaxSize;

	    if (options.blacklistFile != null) {
	    	if (!options.blacklistFile.exists()) {
	    		System.out.println("Blacklist file does not exist!");
	    		System.exit(-1);
	    	}
	    	blacklistMap = new HashSet<String>();
	    	RandomAccessFile raf = null;
	    	String tmpStr;
	    	try {
		    	raf = new RandomAccessFile(options.blacklistFile, "r");
		    	while ((tmpStr = raf.readLine()) != null) {
		    		if (tmpStr.length() > 0) {
		    			blacklistMap.add(tmpStr);
		    		}
		    	}
		    	raf.close();
		    	raf = null;
		    	System.out.println(blacklistMap.size() + " files blacklisted.");
	    	}
	    	catch (IOException e) {
	    		if (raf != null) {
	    			try {
			    		raf.close();
	    			} catch (IOException e1) {
	    			}
		    		raf = null;
	    		}
	    		e.printStackTrace();
	    		System.exit(-1);
	    	}
	    }

	    if (options.checksumsFile != null) {
	    	if (!options.checksumsFile.exists()) {
	    		System.out.println("Checksums file does not exist!");
	    		System.exit(-1);
	    	}
			ffl = new FlatfileLookupCaching(options.checksumsFile, 4, 16);
			ffl.lock();
			try {
				if (!ffl.open()) {
		    		System.out.println("Unable to open checksums file!");
		    		System.exit(-1);
				}
				checksumsRaf = new RandomAccessFile(options.checksumsFile, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
	    }

	    try {
			failsOutput = new SynchronizedOutput("fails.out", 1024*1024);
			exceptionsOutput = new SynchronizedOutput("exceptions.out", 1024*1024);
			compressReportOutput = new SynchronizedOutput("compress-report.out", 1024*1024);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle( options.filesList, options.bQueueFirst, this, options.threads );

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (checksumsRaf != null) {
			try {
				checksumsRaf.close();
			} catch (IOException e) {
			}
			checksumsRaf = null;
		}
		if (ffl != null) {
			ffl.close();
			ffl.unlock();
			ffl = null;
		}

		exceptionsOutput.close();
		failsOutput.close();

		calculate_runstats();

		if (!options.bQuiet) {
			cout.println("         Time: " + run_timestr + " (" + run_dtm + " ms.)" );
			cout.println("   TotalBytes: " + toSizeString(current_size));
			cout.println("     AvgBytes: " + toSizePerSecondString(run_avgbpsec));
			cout.println(String.format("       Gained: %s (%.2f%%).", toSizeString(uncompressed - compressed), current_gain));
			cout.println("    Completed: " + completed);
			cout.println("  Incompleted: " + incomplete);
			cout.println("  Blacklisted: " + blacklisted);
			cout.println("IntegrityFail: " + integrityFail);
			cout.println("ChksumMissing: " + missingChecksum);
			cout.println("  ChksumWrong: " + wrongChecksum);
			Iterator<Entry<String, Long>> schemesIter = schemesMap.entrySet().iterator();
			Entry<String, Long> schemeEntry;
			while (schemesIter.hasNext()) {
				schemeEntry = schemesIter.next();
				cout.println(schemeEntry.getKey() + " (" + schemeEntry.getValue() + ")");
			}
		}

		compressReportOutput.acquire();
		compressReportOutput.out.println("         Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		compressReportOutput.out.println("   TotalBytes: " + toSizeString(current_size));
		compressReportOutput.out.println("     AvgBytes: " + toSizePerSecondString(run_avgbpsec));
		compressReportOutput.out.println(String.format("       Gained: %s (%.2f%%).", toSizeString(uncompressed - compressed), current_gain));
		compressReportOutput.out.println("    Completed: " + completed);
		compressReportOutput.out.println("  Incompleted: " + incomplete);
		compressReportOutput.out.println("  Blacklisted: " + blacklisted);
		compressReportOutput.out.println("IntegrityFail: " + integrityFail);
		compressReportOutput.out.println("ChksumMissing: " + missingChecksum);
		compressReportOutput.out.println("  ChksumWrong: " + wrongChecksum);
		Iterator<Entry<String, Long>> schemesIter = schemesMap.entrySet().iterator();
		Entry<String, Long> schemeEntry;
		while (schemesIter.hasNext()) {
			schemeEntry = schemesIter.next();
			compressReportOutput.out.println(schemeEntry.getKey() + " (" + schemeEntry.getValue() + ")");
		}
		compressReportOutput.release();

		compressReportOutput.close();
	}

	@Override
	public void process(File srcFile) {
		String filename = srcFile.getName();
		if (blacklistMap != null && blacklistMap.contains(filename)) {
			++blacklisted;
			cout.println("File blacklisted: '" + srcFile.getPath() + "'");
		} else {
			FileIdent fileIdent = FileIdent.ident(srcFile);
			boolean bSubmit;
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
					bSubmit = true;
					byte[] expectedDigest = null;
					if (ffl != null) {
						try {
							long pos = ffl.lookup(filename);
							checksumsRaf.seek(pos);
							String tmpStr = checksumsRaf.readLine();
							if (tmpStr != null) {
								int cmp = psComparator.comparePrefix(filename.toCharArray(), tmpStr.toCharArray());
								bSubmit = (cmp == 0);
								int index = tmpStr.indexOf("##");
								if (index != -1) {
									expectedDigest = Base16.decodeToArray(tmpStr.substring(index + 2));
								}
							} else {
								bSubmit = false;
							}
							if (!bSubmit) {
								++missingChecksum;
								cout.println("Checksum not found in reference file for: '" + srcFile.getPath() + "'");
							}
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}
					if (bSubmit) {
						executor.submit(new TaskRunnable(srcFile, expectedDigest));
						queued_size += srcFile.length();
						++queued;
					}
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
	}

	class TaskRunnable implements Runnable {
		File srcFile;
		byte[] expectedDigest;
		TaskRunnable(File srcFile, byte[] expectedDigest) {
			this.srcFile = srcFile;
			this.expectedDigest = expectedDigest;
		}
		@Override
		public void run() {
			CompressFile compressFile = new CompressFile();
			CompressResult compressionResult = compressFile.compressFile(srcFile, expectedDigest, options);
			results.add(compressionResult);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed Compressed results list. */
	private ConcurrentLinkedQueue<CompressResult> results = new ConcurrentLinkedQueue<CompressResult>();

	protected Map<String, Long> schemesMap = new HashMap<String, Long>();

	private long completed = 0;

	private long incomplete = 0;

	private long integrityFail = 0;

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
			Iterator<Entry<String, Long>> schemesIter;
			Entry<String, Long> schemeEntry;
			String scheme;
			Long count;
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

							if (result.schemesMap != null) {
								schemesIter = result.schemesMap.entrySet().iterator();
								while (schemesIter.hasNext()) {
									schemeEntry = schemesIter.next();
									scheme = schemeEntry.getKey();
									count = schemesMap.get(scheme);
									if (count == null) {
										count = 0L;
									}
									schemesMap.put(scheme, count + schemeEntry.getValue());
								}
							}

							if (result.bCompleted) {
								++completed;
								if (options.bVerify) {
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
						        	else {
						        		++integrityFail;
										cout.print("Integrity fail: " + result.srcFile.getPath());
										failsOutput.acquire();
										failsOutput.out.println(result.srcFile.getPath());
										failsOutput.release();
						        	}
						        	if (result.bExpected != null && !result.bExpected) {
						        		++wrongChecksum;
										cout.print("Checksum fail: " + result.srcFile.getPath());
										failsOutput.acquire();
										failsOutput.out.println(result.srcFile.getPath());
										failsOutput.release();
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
							else {
								++incomplete;
								cout.print("Incomplete: " + result.srcFile.getPath());
								failsOutput.acquire();
								failsOutput.out.println(result.srcFile.getPath());
								failsOutput.release();
							}
							if (result.t != null) {
								exceptionsOutput.acquire();
								exceptionsOutput.out.println( "#" );
								exceptionsOutput.out.println( "# Exception while processing '" + result.srcFile + "'" );
								exceptionsOutput.out.println( "#" );
								result.t.printStackTrace( exceptionsOutput.out );
								exceptionsOutput.release();
							}

							if (options.bDryrun) {
								result.dstFile.delete();
							}
							else if (options.bRemove) {
								if (result.bCompleted && result.bVerified && (result.bExpected == null || result.bExpected)) {
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
