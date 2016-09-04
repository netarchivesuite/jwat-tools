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

import org.jwat.archive.FileIdent;
import org.jwat.common.DiagnosisType;
import org.jwat.tools.tasks.ProcessTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class ContainerMDTask extends ProcessTask {

	private ContainerMDOptions options;

	/*
	 * Summary.
	 */

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

	public ContainerMDTask() {
	}

	public void runtask(ContainerMDOptions options) {
		this.options = options;

		ResultThread resultThread = new ResultThread();
		Thread thread = new Thread(resultThread);
		thread.start();

		threadpool_feeder_lifecycle(options.filesList, this, options.threads);

		resultThread.bExit = true;
		while (!resultThread.bClosed) {
			try {
				Thread.sleep( 100 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		calucate_runstats();

		if (!options.bQuiet) {
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
    	  if (!options.bQuiet) cout.println("Wrong extension: '" + srcFile.getPath() + "'");
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
    	  if (!options.bQuiet) cout.println("Empty file: '" + srcFile.getPath() + "'");
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
			ParseContainerMD parseContainerMD = new ParseContainerMD();
			ContainerMDResult result = parseContainerMD.processFile(srcFile, options);
			// FIXME
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
			if (!options.bQuiet) cout.println("Output Thread started.");
			boolean bLoop = true;
			while (bLoop) {
				try {
					if (resultsReady.tryAcquire(1, TimeUnit.SECONDS)) {
						result = results.poll();
						File outputFile = new File(options.outputDir, 
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

						if (!options.bQuiet) {
							cout.print_progress(String.format("Queued: %d - Processed: %d - %s - Estimated: %s (%.2f%%).", queued, processed, toSizePerSecondString(current_avgbpsec), current_timestr, current_progress));
						}
					} else if (bExit && processed == queued) {
						bLoop = false;
					}
				} catch (InterruptedException e) {
					bLoop = false;
				}
			}
			if (!options.bQuiet) cout.println("Output Thread stopped.");

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
