package org.jwat.tools.tasks.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.core.Task;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.tools.validators.XmlValidatorPlugin;

public class TestTask extends Task {

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

	private List<ValidatorPlugin> validatorPlugins = new LinkedList<ValidatorPlugin>();

	private UriProfile uriProfile = UriProfile.RFC3986;

	private int recordHeaderMaxSize = 1024 * 1024;
    private int payloadHeaderMaxSize = 1024 * 1024;

	/*
	 * State.
	 */

	/** Valid results output stream. */
	private SynchronizedOutput validOutput;

	/** Invalid results output stream. */
	private SynchronizedOutput invalidOutput;

	/** Exception output stream. */
	private SynchronizedOutput exceptionsOutput;

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<TestFileResult> results = new ConcurrentLinkedQueue<TestFileResult>();

	public TestTask() {
	}

	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
			System.out.println("Showing errors.");
		}
		if ( arguments.idMap.containsKey( JWATTools.A_LAX ) ) {
			uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}
		if ( arguments.idMap.containsKey( JWATTools.A_XML ) ) {
			validatorPlugins.add(new XmlValidatorPlugin());
		}
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		validOutput = new SynchronizedOutput("v.out");
		invalidOutput = new SynchronizedOutput("i.out");
		exceptionsOutput = new SynchronizedOutput("e.out");

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
		validOutput.out.println( "Validation took " + (System.currentTimeMillis() - startCtm) + " ms." );
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
		invalidOutput.out.println( "Validation took " + (System.currentTimeMillis() - startCtm) + " ms." );
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
		cout.println( "Validation took " + (System.currentTimeMillis() - startCtm) + " ms." );

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
	public void process(File srcFile) {
		if (srcFile.length() > 0) {
			int fileId = FileIdent.identFile(srcFile);
			if (fileId > 0) {
				executor.submit(new TestRunnable(srcFile));
				++queued;
			} else {
			}
		}
	}

	class TestRunnable implements Runnable {
		File srcFile;
		TestRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			TestFile2 testFile = new TestFile2();
			testFile.bShowErrors = bShowErrors;
			testFile.uriProfile = uriProfile;
		    testFile.recordHeaderMaxSize = recordHeaderMaxSize;
		    testFile.payloadHeaderMaxSize = payloadHeaderMaxSize;
			testFile.validatorPlugins = validatorPlugins;
			testFile.callback = null;
			TestFileResult result = testFile.processFile(srcFile);
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

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			TestFileResult result;
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
						}
						catch (Throwable t) {
							++result.runtimeErrors;
							t.printStackTrace();
						}
						exceptionsOutput.release();
						invalidOutput.release();
						validOutput.release();
						update_summary(result);
						++processed;
						cout.print_progress("Queued: " + queued + " - Processed: " + processed + ".");
					} else if (bExit) {
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
