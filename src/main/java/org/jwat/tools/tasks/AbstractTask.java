package org.jwat.tools.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.antiaction.common.cli.ProgressableOutput;
import com.antiaction.common.cli.WildcardMatcher;

public abstract class AbstractTask {

	/** ThreadPool executor. */
	public ThreadPoolExecutorPausable executor; 

	public ProgressableOutput cout = new ProgressableOutput(System.out);

	/** Thread pool processing start time. */
	public long startCtm;

	/** Size of queue files for estimation of completion. */
    public long queued_size;

    /** Size of processed files for estimation of completion. */
    public long current_size;

    /** Files queued. */
	public int queued = 0;

	/** Files processed. */
	public int processed = 0;

	public abstract void process(File file);

	//private List<Future<TestResult>> futures = new LinkedList<Future<TestResult>>();
	/*
	Future<TestResult> future = executor.submit(new TestCallable(srcFile));
	Future<?> future = executor.submit(new TestRunnable(srcFile));
	futures.add(future);
	 */

	public void threadpool_feeder_lifecycle(List<String> filesList, boolean bQueueFirst, AbstractTask task, int threads) {
		cout.println( "Using " + threads + " thread(s)." );
		//executor = Executors.newFixedThreadPool(16);
		executor = new ThreadPoolExecutorPausable(threads, threads, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		if (bQueueFirst) {
			executor.pause();
		}
		startCtm = System.currentTimeMillis();
		cout.println("ThreadPool started.");
		try {
			filelist_feeder( filesList, task );
		} catch (Throwable t) {
			cout.println("Died unexpectedly!");
		} finally {
			cout.println("Queued " + queued + " file(s).");
			if (bQueueFirst) {
				startCtm = System.currentTimeMillis();
				executor.resume();
			}
			//System.out.println("Queued: " + queued + " - Processed: " + processed + ".");
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

	public void filelist_feeder(List<String> filesList, AbstractTask task) {
		String fileSeparator = System.getProperty( "file.separator" );
		File parentFile;
		String filepart;
		FileFilter filter;
		for ( int i=0; i<filesList.size(); ++i ) {
			filepart = filesList.get( i );
			int idx = filepart.lastIndexOf( fileSeparator );
			if ( idx != -1 ) {
				idx += fileSeparator.length();
				parentFile = new File( filepart.substring( 0, idx ) );
				filepart = filepart.substring( idx );
			}
			else {
				parentFile = new File( System.getProperty( "user.dir" ) );
			}
			idx = filepart.indexOf( "*" );
			if ( idx == -1 ) {
				parentFile = new File( parentFile, filepart );
				filepart = "";
				filter = new AcceptAllFileFilter();
			}
			else {
				filter = new AcceptWildcardFileFilter(filepart);
			}
			if ( parentFile.exists() ) {
				filelist_feeder_process( parentFile, filter, task );
			}
			else {
				cout.println( "File does not exist -- " + parentFile.getPath() );
				System.exit( 1 );
			}
		}
	}

	protected void filelist_feeder_process(File parentFile, FileFilter filter, AbstractTask task) {
		if ( parentFile.isFile() ) {
			task.process( parentFile );
		}
		else if ( parentFile.isDirectory() ) {
			File[] files = parentFile.listFiles( filter );
			if (files != null) {
				for ( int i=0; i<files.length; ++i ) {
					if ( files[ i ].isFile() ) {
						task.process( files[ i ] );
					}
					else {
						filelist_feeder_process( files[ i ], filter, task );
					}
				}
			}
			else {
				cout.println("Error reading: " + parentFile.getPath());
			}
		}
	}

	static class AcceptAllFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return true;
		}
	}

	static class AcceptAllFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return true;
		}
	}

	static class AcceptWildcardFileFilter implements FileFilter {
		private WildcardMatcher wm;
		public AcceptWildcardFileFilter(String pattern) {
			wm = new WildcardMatcher(pattern);
		}
		@Override
		public boolean accept(File pathname) {
			if (pathname.isFile()) {
				return wm.match(pathname.getName());
			}
			return true;
		}
	}

	public double current_progress;
	public String current_timestr;
	public long current_avgbpsec;

	public void calculate_progress() {
		long ctm = System.currentTimeMillis();
		long dtm = ctm - startCtm;
		current_progress = (double)current_size / (double)queued_size * 100.0;
		long etm = (long)((double)dtm / current_progress * 100.0);
		long rtm = etm - dtm;

		long rts = rtm / 1000;
		if (rts > 0) {
			long l = rts;
			long l3 = (l % 60);
			l = l / 60;
			long l2 = (l % 60);
			l = l / 60;
			current_timestr = String.format("%02d:%02d:%02d", l, l2, l3);
		}
		else {
			current_timestr = "--:--:--";
		}

		long dts = dtm / 1000;
		if (dts > 0) {
			current_avgbpsec = current_size / dts;
		} else {
			current_avgbpsec = current_size;
		}
	}

	public long run_dtm;
	public long run_avgbpsec;
	public String run_timestr;

	public void calculate_runstats() {
		run_dtm = System.currentTimeMillis() - startCtm;
		long run_dts = run_dtm / 1000;
		if (run_dts > 0) {
			run_avgbpsec = current_size / run_dts;
		}
		long l = run_dts;
		long l3 = (l % 60);
		l = l / 60;
		long l2 = (l % 60);
		l = l / 60;
		run_timestr = String.format("%02d:%02d:%02d", l, l2, l3);
	}

	public static String toSizeString(long l) {
		StringBuffer strBuf = new StringBuffer();
		if ( l < (1024*1024) ) {
			strBuf.append( Long.toString( (l * 10) / 1024 ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " kb" );
			return strBuf.toString();
		}
		else if ( l < (1024*1024*1024) ) {
			strBuf.append( Long.toString( (l * 10) / (1024*1024) ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " mb" );
			return strBuf.toString();
		}
		else {
			strBuf.append( Long.toString( (l * 10) / (1024*1024*1024) ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " gb" );
			return strBuf.toString();
		}
	}

	public static String toSizePerSecondString(long l) {
		StringBuffer strBuf = new StringBuffer();
		if ( l < (1024*1024) ) {
			strBuf.append( Long.toString( (l * 10) / 1024 ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " kb/s" );
			return strBuf.toString();
		}
		else if ( l < (1024*1024*1024) ) {
			strBuf.append( Long.toString( (l * 10) / (1024*1024) ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " mb/s" );
			return strBuf.toString();
		}
		else {
			strBuf.append( Long.toString( (l * 10) / (1024*1024*1024) ) );
			if ( strBuf.length() == 1 ) {
				strBuf.insert( 0, "0" );
			}
			strBuf.insert( strBuf.length() - 1, "." );
			strBuf.append( " gb/s" );
			return strBuf.toString();
		}
	}

	public static String toByteSizeString(long l) {
		StringBuffer strBuf = new StringBuffer( Long.toString( l ) );
		int idx = strBuf.length() - 3;
		while ( idx > 0 ) {
			strBuf.insert( idx, "." );
			idx -= 3;
		}
		strBuf.append( " bytes" );
		return strBuf.toString();
	}

}
