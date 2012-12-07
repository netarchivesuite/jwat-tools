package org.jwat.tools.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class Task {

	/** Threads to use in thread pool. */
	public int threads = 1;

	/** ThreadPool executor. */
	public ExecutorService executor; 

	/** Thread pool processing start time. */
	public long startCtm;

	public ProgressableOutput cout = new ProgressableOutput(System.out);

	public int queued = 0;

	public int processed = 0;

	public abstract void command(CommandLine.Arguments arguments);

	public abstract void process(File file);

	//private List<Future<TestResult>> futures = new LinkedList<Future<TestResult>>();
	/*
	Future<TestResult> future = executor.submit(new TestCallable(srcFile));
	Future<?> future = executor.submit(new TestRunnable(srcFile));
	futures.add(future);
	 */

	public void threadpool_feeder_lifecycle(List<String> filesList, Task task) {
		cout.println( "Using " + threads + " thread(s)." );
		//executor = Executors.newFixedThreadPool(16);
		executor = new ThreadPoolExecutor(threads, threads, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		startCtm = System.currentTimeMillis();
		cout.println("ThreadPool started.");
		try {
			filelist_feeder( filesList, task );
		} catch (Throwable t) {
			cout.println("Died unexpectedly!");
		} finally {
			cout.println("Queued " + queued + " file(s).");
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

	public void filelist_feeder(List<String> filesList, Task task) {
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

	public void filelist_feeder_process(File parentFile, FileFilter filter, Task task) {
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

}
