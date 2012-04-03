package org.jwat.tools;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

	private int queued = 0;
	private int processed = 0;

	private boolean bShowErrors = false;

	private ProgressableOutput cout = new ProgressableOutput(System.out);

	/** Validation output stream. */
	private SynchronizedOutput validationOutput;

	/** Exception output stream. */
	private SynchronizedOutput exceptionOutput;

	/** ThreadPool executor. */
	private ExecutorService executor; 

	//private List<Future<TestResult>> futures = new LinkedList<Future<TestResult>>();

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<TestFileResult> results = new ConcurrentLinkedQueue<TestFileResult>();

	public TestTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
		}
		int threads = 1;
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
				System.out.println( "Using " + threads + " thread(s)." );
			} catch (NumberFormatException e) {
			}
		}
		//executor = Executors.newFixedThreadPool(16);
		argument = arguments.idMap.get( JWATTools.A_FILES );
		executor = new ThreadPoolExecutor(threads, threads, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()); 
		cout.println("ThreadPool started.");
		validationOutput = new SynchronizedOutput("v.out");
		exceptionOutput = new SynchronizedOutput("e.out");
		Thread thread = new Thread(new OutputThread());
		thread.start();
		long startCtm = System.currentTimeMillis();
		try {
			List<String> filesList = argument.values;
			taskFileListFeeder( filesList, this );
		} catch (Throwable t) {
			cout.println("Died unexpectedly!");
		} finally {
			cout.println("Queued " + queued + " validation job(s).");
			//System.out.println("Queued: " + queued + " - Processed: " + processed + ".");
			if (executor != null) {
				executor.shutdown();
				/*
				try {
					executor.awaitTermination(60L, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
				}
				*/
				while (!executor.isTerminated()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				cout.println("ThreadPool shut down.");
				thread.interrupt();
				/*
				Iterator<Future<TestResult>> iter = futures.iterator();
				Future<TestResult> future;
				TestResult result;
				while (iter.hasNext()) {
					future = iter.next();
					if (future.isDone()) {
						try {
							result = future.get();
							update_summary(result);
						} catch (CancellationException e) {
						} catch (ExecutionException e) {
						} catch (InterruptedException e) {
						}
					} else {
						System.out.println("NOOOOOOOOOOOOOOOOOOOOOOO!");
					}
				}
				*/
			}
		}
		validationOutput.close();
		exceptionOutput.close();
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
	}

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
	}

	@Override
	public void process(File srcFile) {
		if (srcFile.length() > 0) {
				boolean bValidate = TestFile.checkfile(srcFile);
				if (bValidate) {
					/*
					Future<TestResult> future = executor.submit(new TestCallable(srcFile));
					futures.add(future);
					*/
					Future<?> future = executor.submit(new TestRunnable(srcFile));
					//futures.add(future);
					++queued;
				} else {
				}
		}
	}

	class OutputThread implements Runnable {

		boolean exit = false;

		@Override
		public void run() {
			TestFileResult result;
			cout.println("Output Thread started.");
			while (!exit) {
				try {
					resultsReady.acquire();
					result = results.poll();
					if (result != null) {
						update_summary(result);
						validationOutput.acquired();
						exceptionOutput.acquired();
						try {
							result.printResult(bShowErrors, validationOutput.out, exceptionOutput.out);
						}
						catch (Throwable t) {
							++result.runtimeErrors;
							t.printStackTrace();
						}
						exceptionOutput.release();
						validationOutput.release();
					} else {
						exit = true;
					}
					++processed;
					cout.print_progress("Queued: " + queued + " - Processed: " + processed + ".");
				} catch (InterruptedException e) {
					exit = true;
				}
			}
			cout.println("Output Thread stopped.");
		}
	}

	class TestCallable implements Callable<TestFileResult> {
		File srcFile;
		TestCallable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public TestFileResult call() throws Exception {
			TestFileResult result = TestFile.processFile(srcFile, bShowErrors, null);
			results.add(result);
			resultsReady.release();
			return result;
		}
	}

	class TestRunnable implements Runnable {
		File srcFile;
		TestRunnable(File srcFile) {
			this.srcFile = srcFile;
		}
		@Override
		public void run() {
			TestFileResult result = TestFile.processFile(srcFile, bShowErrors, null);
			results.add(result);
			resultsReady.release();
		}
	}

}
