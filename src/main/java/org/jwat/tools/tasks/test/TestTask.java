package org.jwat.tools.tasks.test;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jwat.common.Diagnosis;
import org.jwat.common.DiagnosisType;
import org.jwat.common.UriProfile;
import org.jwat.tools.JWATTools;
import org.jwat.tools.core.Cloner;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.tools.tasks.ProcessTask;
import org.jwat.tools.validators.XmlValidatorPlugin;

public class TestTask extends ProcessTask {

	public static final String commandName = "test";

	public static final String commandDescription = "test validity of ARC/WARC/GZip file(s)";

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

	private boolean bShowErrors = false;

	private boolean bValidateDigest = true;

	private Long after = 0L;

	private boolean bBad = false;

	private List<ValidatorPlugin> validatorPlugins = new LinkedList<ValidatorPlugin>();

	private UriProfile uriProfile = UriProfile.RFC3986;

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

	@Override
	public void show_help() {
		System.out.println("jwattools test [-elx] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("test one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -a                  after yyyyMMddHHmmss");
		System.out.println(" -b                  tag bad files (*.bad)");
		System.out.println(" -e                  show errors");
		System.out.println(" -i --ignore-digest  skip digest calculation and validation");
		System.out.println(" -l                  relaxed URL URI validation");
		System.out.println(" -x                  to validate text/xml payload (eg. mets)");
		System.out.println(" -w<x>               set the amount of worker thread(s) (defaults to 1)");
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

		// Show errors.
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
		}
		System.out.println("Showing errors: " + bShowErrors);

		// Ignore digest.
		if ( arguments.idMap.containsKey( JWATTools.A_IGNORE_DIGEST ) ) {
			bValidateDigest = false;
		}
		System.out.println("Validate digest: " + bValidateDigest);

		// Relaxed URI validation.
		if ( arguments.idMap.containsKey( JWATTools.A_LAX ) ) {
			uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}

		// XML validation.
		if ( arguments.idMap.containsKey( JWATTools.A_XML ) ) {
			validatorPlugins.add(new XmlValidatorPlugin());
		}

		// Tag.
		if ( arguments.idMap.containsKey( JWATTools.A_BAD ) ) {
			bBad = true;
			System.out.println("Tagging enabled for invalid files");
		}

		// After.
		argument = arguments.idMap.get( JWATTools.A_AFTER );
		if ( argument != null && argument.value != null ) {
			try {
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		        dateFormat.setLenient(false);
		        Date afterDate = dateFormat.parse(argument.value);
		        after = afterDate.getTime();
			} catch (ParseException e) {
				System.out.println("Invalid date format - " + argument.value);
			}
		}

        // Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		validOutput = new SynchronizedOutput("v.out");
		invalidOutput = new SynchronizedOutput("i.out");
		exceptionsOutput = new SynchronizedOutput("e.out");

		// TODO optional
		//cloner = Cloner.getCloner();

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

		calucate_runstats();

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
		if (srcFile.lastModified() > after) {
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
		TaskRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			TestFile2 testFile = new TestFile2();
			testFile.bShowErrors = bShowErrors;
			testFile.bValidateDigest = bValidateDigest;
			testFile.uriProfile = uriProfile;
		    testFile.recordHeaderMaxSize = recordHeaderMaxSize;
		    testFile.payloadHeaderMaxSize = payloadHeaderMaxSize;
			testFile.validatorPlugins = validatorPlugins;
			testFile.callback = null;
			TestFileResult result = testFile.processFile(srcFile, cloner);
			result.srcFile = srcFile;
			results.add(result);
			resultsReady.release();
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
							result.printResult(bShowErrors, validOutput.out, invalidOutput.out, exceptionsOutput.out);
							if (bBad) {
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
