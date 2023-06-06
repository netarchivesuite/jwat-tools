package org.jwat.tools.tasks.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.archive.Cloner;
import org.jwat.archive.FileIdent;
import org.jwat.common.Diagnosis;
import org.jwat.common.DiagnosisType;
import org.jwat.tools.tasks.AbstractTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class TestTask extends AbstractTask {

	private TestOptions options;

	/*
	 * Summary.
	 */

	private int arcGzFiles = 0;
	private int warcGzFiles = 0;
	private int gzFiles = 0;
	private int arcFiles = 0;
	private int warcFiles = 0;
	private int errors = 0;
	private int warnings = 0;
	private int runtimeErrors = 0;
	private int skipped = 0;

	/*
	 * Settings.
	 */

	private int recordHeaderMaxSize = 1024 * 1024;
    private int payloadHeaderMaxSize = 1024 * 1024;

    private Cloner cloner;

    /*
	 * State.
	 */

    /** Valid results output stream. */
	private SynchronizedOutput validOutput;

	/** Invalid results output stream. */
	private SynchronizedOutput invalidOutput;

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

	public TestTask() {
	}

	public void runtask(TestOptions options) {
		this.options = options;
	    options.recordHeaderMaxSize = recordHeaderMaxSize;
	    options.payloadHeaderMaxSize = payloadHeaderMaxSize;

		// TODO optional
		//cloner = Cloner.getCloner();

		try {
			validOutput = new SynchronizedOutput("v.out", 1024*1024);
			invalidOutput = new SynchronizedOutput("i.out", 1024*1024);
			exceptionsOutput = new SynchronizedOutput("e.out", 1024*1024);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

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

		calculate_runstats();

		if (cloner != null) {
			try {
				cloner.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			cloner = null;
		}

		exceptionsOutput.close();

		validOutput.acquire();
		validOutput.out.println( "#" );
		validOutput.out.println( "# Job summary" );
		validOutput.out.println( "#" );
		validOutput.out.println( "GZip files: " + gzFiles );
		validOutput.out.println( "  +  Arc: " + arcGzFiles );
		validOutput.out.println( "  + Warc: " + warcGzFiles );
		validOutput.out.println( " Arc files: " + arcFiles );
		validOutput.out.println( "Warc files: " + warcFiles );
		validOutput.out.println( "    Errors: " + errors );
		validOutput.out.println( "  Warnings: " + warnings );
		validOutput.out.println( "RuntimeErr: " + runtimeErrors );
		validOutput.out.println( "   Skipped: " + skipped );
		validOutput.out.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		validOutput.out.println( "TotalBytes: " + toSizeString(current_size));
		validOutput.out.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));
		validOutput.release();
		validOutput.close();

		invalidOutput.acquire();
		invalidOutput.out.println( "#" );
		invalidOutput.out.println( "# Job summary" );
		invalidOutput.out.println( "#" );
		invalidOutput.out.println( "GZip files: " + gzFiles );
		invalidOutput.out.println( "  +  Arc: " + arcGzFiles );
		invalidOutput.out.println( "  + Warc: " + warcGzFiles );
		invalidOutput.out.println( " Arc files: " + arcFiles );
		invalidOutput.out.println( "Warc files: " + warcFiles );
		invalidOutput.out.println( "    Errors: " + errors );
		invalidOutput.out.println( "  Warnings: " + warnings );
		invalidOutput.out.println( "RuntimeErr: " + runtimeErrors );
		invalidOutput.out.println( "   Skipped: " + skipped );
		invalidOutput.out.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		invalidOutput.out.println( "TotalBytes: " + toSizeString(current_size));
		invalidOutput.out.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));
		invalidOutput.release();
		invalidOutput.close();

		cout.println( "#" );
		cout.println( "# Job summary" );
		cout.println( "#" );
		cout.println( "GZip files: " + gzFiles );
		cout.println( "  +  Arc: " + arcGzFiles );
		cout.println( "  + Warc: " + warcGzFiles );
		cout.println( " Arc files: " + arcFiles );
		cout.println( "Warc files: " + warcFiles );
		cout.println( "    Errors: " + errors );
		cout.println( "  Warnings: " + warnings );
		cout.println( "RuntimeErr: " + runtimeErrors );
		cout.println( "   Skipped: " + skipped );
		cout.println( "      Time: " + run_timestr + " (" + run_dtm + " ms.)" );
		cout.println( "TotalBytes: " + toSizeString(current_size));
		cout.println( "  AvgBytes: " + toSizePerSecondString(run_avgbpsec));

		List<Entry<DiagnosisType, Integer>> typeNumbersList = new ArrayList<Entry<DiagnosisType, Integer>>(typeNumbers.entrySet());
		//Collections.sort(typeNumbersList, new EntryDiagnosisTypeComparator());
		Collections.sort(typeNumbersList, new EntryComparator<DiagnosisType>());
		Entry<DiagnosisType, Integer> typeNumberEntry;
		for (int i=0; i<typeNumbersList.size(); ++i) {
			typeNumberEntry = typeNumbersList.get(i);
			System.out.println(typeNumberEntry.getKey().toString() + ": " + typeNumberEntry.getValue());
		}

		List<Entry<String, Integer>> entityNumbersList = new ArrayList<Entry<String, Integer>>(entityNumbers.entrySet());
		//Collections.sort(entityNumbersList, new EntryStringComparator());
		Collections.sort(entityNumbersList, new EntryComparator<String>());
		Entry<String, Integer> entityNumberEntry;
		for (int i=0; i<entityNumbersList.size(); ++i) {
			entityNumberEntry = entityNumbersList.get(i);
			System.out.println(entityNumberEntry.getKey().toString() + ": " + entityNumberEntry.getValue());
		}
	}

	public class EntryComparator<K extends Comparable<? super K>> implements Comparator<Map.Entry<K, ?>> {
		@Override
		public int compare(Map.Entry<K, ?> o1, Map.Entry<K, ?> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

	/*
	public class EntryDiagnosisTypeComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			Entry<DiagnosisType, Integer> e1 = (Entry<DiagnosisType, Integer>)o1;
			Entry<DiagnosisType, Integer> e2 = (Entry<DiagnosisType, Integer>)o2;
			return e1.getKey().compareTo(e2.getKey());
		}
	}

	public class EntryStringComparator implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			Entry<String, Integer> e1 = (Entry<String, Integer>)o1;
			Entry<String, Integer> e2 = (Entry<String, Integer>)o2;
			return e1.getKey().compareTo(e2.getKey());
		}
	}
	*/

	@Override
	public synchronized void process(File srcFile) {
		if (srcFile.lastModified() > options.after) {
			FileIdent fileIdent = FileIdent.ident(srcFile);
			if (srcFile.length() > 0) {
				// debug
				//System.out.println(fileIdent.filenameId + " " + fileIdent.streamId + " " + srcFile.getName());
				if (fileIdent.filenameId != fileIdent.streamId) {
					cout.println("Wrong extension: '" + srcFile.getPath() + "'");
				}
				switch (fileIdent.streamId) {
				case FileIdent.FILEID_GZIP:
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
				case FileIdent.FILEID_GZIP:
				case FileIdent.FILEID_ARC:
				case FileIdent.FILEID_ARC_GZ:
				case FileIdent.FILEID_WARC:
				case FileIdent.FILEID_WARC_GZ:
					//cout.println("Empty file: '" + srcFile.getPath() + "'");
					executor.submit(new TaskRunnable(srcFile));
					queued_size += srcFile.length();
					++queued;
					break;
				default:
					break;
				}
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
			try {
				TestFile2 testFile = new TestFile2();
				testFile.callback = null;
				TestFileResult result = testFile.processFile(srcFile, options, cloner);
				results.add(result);
				resultsReady.release();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/*
	class TestCallable implements Callable<TestFileResult> {
		File srcFile;
		TestCallable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public TestFileResult call() throws Exception {
			TestFileResult result = testFile.processFile(srcFile, bShowErrors, null);
			results.add(result);
			resultsReady.release();
			return result;
		}
	}
	*/

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<TestFileResult> results = new ConcurrentLinkedQueue<TestFileResult>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			TestFileResult result;
			File newFile;
			cout.println("Output Thread started.");
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						validOutput.acquire();
						invalidOutput.acquire();
						exceptionsOutput.acquire();
						try {
							result.printResult(options.bShowErrors, validOutput.out, invalidOutput.out, exceptionsOutput.out);
							if (options.bBad) {
								if (result.rdList.size() > 0 || result.throwableList.size() > 0) {
									if (!result.srcFile.getName().endsWith(".bad")) {
										newFile = new File(result.srcFile.getParent(), result.srcFile.getName() + ".bad");
										if (!result.srcFile.renameTo(newFile)) {
											cout.println(String.format("Could not renamed '%s' to '%s'", result.srcFile.getPath(), newFile.getPath()));
										}
									}
								}
							}
						}
						catch (Throwable t) {
							++result.runtimeErrors;
							t.printStackTrace();
						}
						exceptionsOutput.release();
						invalidOutput.release();
						validOutput.release();
						update_summary(result);
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
			cout.println("Output Thread stopped.");

			bClosed = true;
		}
	}

	Map<DiagnosisType, Integer> typeNumbers = new TreeMap<DiagnosisType, Integer>();

	Map<String, Integer> entityNumbers = new HashMap<String, Integer>();

	public void update_summary(TestFileResult result) {
		arcGzFiles += result.arcGzFiles;
		warcGzFiles += result.warcGzFiles;
		gzFiles += result.gzFiles;
		arcFiles += result.arcFiles;
		warcFiles += result.warcFiles;
		runtimeErrors += result.runtimeErrors;
		skipped += result.skipped;
		errors += result.gzipErrors;
		warnings += result.gzipWarnings;
		errors += result.arcErrors;
		warnings += result.arcWarnings;
		errors += result.warcErrors;
		warnings += result.warcWarnings;

		List<TestFileResultItemDiagnosis> resultDiagnoses = result.rdList;
		TestFileResultItemDiagnosis resultDiagnosis;
		List<Diagnosis> diagnoses;
		Diagnosis diagnosis;
		Integer number;
		if (resultDiagnoses != null) {
			for (int i=0; i<resultDiagnoses.size(); ++i) {
				resultDiagnosis = resultDiagnoses.get(i);
				diagnoses = resultDiagnosis.errors;
				for (int j=0; j<diagnoses.size(); ++j) {
					diagnosis = diagnoses.get(j);
					number = typeNumbers.get(diagnosis.type);
					if (number == null) {
						number = 0;
					}
					++number;
					typeNumbers.put(diagnosis.type, number);
					number = entityNumbers.get(diagnosis.entity);
					if (number == null) {
						number = 0;
					}
					++number;
					entityNumbers.put(diagnosis.entity, number);
				}
			}
		}
	}

}
