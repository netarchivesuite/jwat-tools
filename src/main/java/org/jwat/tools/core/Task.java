package org.jwat.tools.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;


public abstract class Task {

	public abstract void process(File file);

	public static void taskFileListFeeder(List<String> filesList, Task task) {
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
				taskFileFeeder( parentFile, filter, task );
			}
			else {
				System.out.println( "File does not exist -- " + parentFile.getPath() );
				System.exit( 1 );
			}
		}
	}

	public static void taskFileFeeder(File parentFile, FileFilter filter, Task task) {
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
						taskFileFeeder( files[ i ], filter, task );
					}
				}
			}
			else {
				System.out.println("Error reading: " + parentFile.getPath());
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
