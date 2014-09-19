package org.jwat.tools.tasks.containermd;

import java.io.File;
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

import org.jwat.common.DiagnosisType;
import org.jwat.common.UriProfile;
import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.tasks.ProcessTask;

public class ContainerMDTask extends ProcessTask {

	public static final String commandName = "containermd";

	public static final String commandDescription = "generation of containerMD for (W)ARC file(s)";

	/*
	 * Summary.
	 */
	private boolean bQuiet = false;
	
	private int arcGzFiles = 0;
	private int warcGzFiles = 0;
	private int arcFiles = 0;
	private int warcFiles = 0;
	private int errors = 0;
	private int warnings = 0;
	private int runtimeErrors = 0;
	private int skipped = 0;

	/*
	 * Settings.
	 */
	private UriProfile uriProfile = UriProfile.RFC3986; // UriProfile.RFC3986_ABS_16BIT_LAX; // UriProfile.RFC3986;
	private File outputDir = new File( System.getProperty("user.dir"));
	private int recordHeaderMaxSize = 1024 * 1024;
    private int payloadHeaderMaxSize = 1024 * 1024;

	public ContainerMDTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools containermd [-d outputDir] [-l] [-q] [-w THREADS] <paths>");
		System.out.println("");
		System.out.println("generate containerMD for (W)ARC files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -d <dir>        destination directory (defaults to current dir)");
		System.out.println(" -l                  relaxed URL URI validation");
		System.out.println(" -q                  quiet, no output to console");
		System.out.println(" -w<x>               set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		bQuiet = arguments.idMap.containsKey( JWATTools.A_QUIET );
		// Thread workers.
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.err.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( threads < 1 ) {
			System.err.println( "Invalid number of threads requested: " + threads );
			System.exit( 1 );
		}

		// Output directory
		argument = arguments.idMap.get( JWATTools.A_DEST );
		if ( argument != null && argument.value != null ) {
			File dir = new File(argument.value);
			if (dir.exists()) {
				if (dir.isDirectory()) {
					outputDir = dir;
				} else {
					if (!bQuiet) System.err.println("Output '" + argument.value + "' invalid, defaulting to '" + outputDir + "'");
				}
			} else {
				if (dir.mkdirs()) {
					outputDir = dir;
				} else {
					if (!bQuiet) System.err.println("Output '" + argument.value + "' invalid, defaulting to '" + outputDir + "'");
				}
			}
		}
		
		// Relaxed URI validation.
		if ( arguments.idMap.containsKey( JWATTools.A_LAX ) ) {
			uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			if (!bQuiet) System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}

        // Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

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

		if (!bQuiet) {
    		cout.println( "#" );
    		cout.println( "# Job summary" );
    		cout.println( "#" );
    		cout.println( "   GzipArc: " + arcGzFiles );
    		cout.println( "  GZipWarc: " + warcGzFiles );
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
    		Collections.sort(typeNumbersList, new EntryComparator<DiagnosisType>());
    		Entry<DiagnosisType, Integer> typeNumberEntry;
    		for (int i=0; i<typeNumbersList.size(); ++i) {
    			typeNumberEntry = typeNumbersList.get(i);
    			cout.println(typeNumberEntry.getKey().toString() + ": " + typeNumberEntry.getValue());
    		}
    
    		List<Entry<String, Integer>> entityNumbersList = new ArrayList<Entry<String, Integer>>(entityNumbers.entrySet());
    		Collections.sort(entityNumbersList, new EntryComparator<String>());
    		Entry<String, Integer> entityNumberEntry;
    		for (int i=0; i<entityNumbersList.size(); ++i) {
    			entityNumberEntry = entityNumbersList.get(i);
    			cout.println(entityNumberEntry.getKey().toString() + ": " + entityNumberEntry.getValue());
    		}
		}
	}

	public class EntryComparator<K extends Comparable<? super K>> implements Comparator<Map.Entry<K, ?>> {
		@Override
		public int compare(Map.Entry<K, ?> o1, Map.Entry<K, ?> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

	@Override
	public synchronized void process(File srcFile) {
    FileIdent fileIdent = FileIdent.ident(srcFile);
    if (srcFile.length() > 0) {
      // debug
      //System.out.println(fileIdent.filenameId + " " + fileIdent.streamId + " " + srcFile.getName());
      if (fileIdent.filenameId != fileIdent.streamId) {
    	  if (!bQuiet) cout.println("Wrong extension: '" + srcFile.getPath() + "'");
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
    	  if (!bQuiet) cout.println("Empty file: '" + srcFile.getPath() + "'");
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
			ParseContainerMD testFile = new ParseContainerMD();
			testFile.uriProfile = uriProfile;
			testFile.bQuiet = bQuiet;
		    testFile.recordHeaderMaxSize = recordHeaderMaxSize;
		    testFile.payloadHeaderMaxSize = payloadHeaderMaxSize;
			//testFile.callback = null;
			ContainerMDResult result = testFile.processFile(srcFile);
			result.srcFile = srcFile;
			result.srcFileSize = srcFile.length();
			
			results.add(result);
			resultsReady.release();
		}
	}

	/** Results ready resource semaphore. */
	private Semaphore resultsReady = new Semaphore(0);

	/** Completed validation results list. */
	private ConcurrentLinkedQueue<ContainerMDResult> results = new ConcurrentLinkedQueue<ContainerMDResult>();

	class ResultThread implements Runnable {

		boolean bExit = false;

		boolean bClosed = false;

		@Override
		public void run() {
			ContainerMDResult result;
			if (!bQuiet) cout.println("Output Thread started.");
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						File outputFile = new File(outputDir, 
								result.srcFile.getName().replaceFirst("\\.w?arc(\\.gz)?", ".containerMD.xml"));
						SynchronizedOutput validOutput = 
								new SynchronizedOutput(outputFile);

						validOutput.acquire();
						try {
							result.printResult(validOutput.out);
						}
						catch (Throwable t) {
							++result.runtimeErrors;
							t.printStackTrace();
						}
						validOutput.release();
						update_summary(result);
						current_size += result.srcFile.length();
						++processed;

						calculate_progress();

						if (!bQuiet) cout.print_progress(String.format("Queued: %d - Processed: %d - %s - Estimated: %s (%.2f%%).", queued, processed, toSizePerSecondString(current_avgbpsec), current_timestr, current_progress));
					} else if (bExit && processed == queued) {
						bLoop = false;
					}
				} catch (InterruptedException e) {
					bLoop = false;
				}
			}
			if (!bQuiet) cout.println("Output Thread stopped.");

			bClosed = true;
		}
	}

	Map<DiagnosisType, Integer> typeNumbers = new TreeMap<DiagnosisType, Integer>();

	Map<String, Integer> entityNumbers = new HashMap<String, Integer>();

	public void update_summary(ContainerMDResult result) {
		arcGzFiles += result.arcGzFiles;
		warcGzFiles += result.warcGzFiles;
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

}
