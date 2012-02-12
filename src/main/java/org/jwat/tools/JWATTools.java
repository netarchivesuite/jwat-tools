package org.jwat.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class JWATTools {

	public static final int A_DECOMPRESS = 1;
	public static final int A_COMPRESS = 2;
	public static final int A_FILES = 3;
	public static final int A_TEST = 4;
	public static final int A_SHOW_ERRORS = 5;

	public static void main(String[] args) {
		JWATTools tools = new JWATTools();
		tools.Main( args );
	}

	public void Main(String[] args) {
		CommandLine.Arguments arguments = null;
		CommandLine cmdLine = new CommandLine();
		cmdLine.addOption( "-d", A_DECOMPRESS );
		cmdLine.addOption( "--decompress", A_DECOMPRESS );
		cmdLine.addOption( "-1", A_COMPRESS, 1 );
		cmdLine.addOption( "-2", A_COMPRESS, 2 );
		cmdLine.addOption( "-3", A_COMPRESS, 3 );
		cmdLine.addOption( "-4", A_COMPRESS, 4 );
		cmdLine.addOption( "-5", A_COMPRESS, 5 );
		cmdLine.addOption( "-6", A_COMPRESS, 6 );
		cmdLine.addOption( "-7", A_COMPRESS, 7 );
		cmdLine.addOption( "-8", A_COMPRESS, 8 );
		cmdLine.addOption( "-9", A_COMPRESS, 9 );
		cmdLine.addOption( "--fast", A_COMPRESS, 1 );
		cmdLine.addOption( "--best", A_COMPRESS, 9 );
		cmdLine.addOption( "-t", A_TEST );
		cmdLine.addOption( "-e", A_SHOW_ERRORS );
		cmdLine.addOption( "--test", A_TEST );
		cmdLine.addListArgument( "files", A_FILES, 1, Integer.MAX_VALUE );
		try {
			arguments = cmdLine.parse( args );
			/*
			for ( int i=0; i<arguments.switchArgsList.size(); ++i) {
				argument = arguments.switchArgsList.get( i );
				System.out.println( argument.argDef.id + "," + argument.argDef.subId + "=" + argument.value );
			}
			*/
		}
		catch (CommandLine.ParseException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}
		if ( arguments == null ) {
			System.out.println( "JWATTools v0.1.0" );
			/*
			System.out.println( "usage: JWATTools [-dt19] [file ...]" );
			System.out.println( " -t --test        test compressed file integrity" );
			System.out.println( " -d --decompress  decompress" );
			System.out.println( " -1 --fast        compress faster" );
			System.out.println( " -9 --best        compress better" );
			*/
			System.out.println( "usage: JWATTools [-dte] [file ...]" );
			System.out.println( " -t   test validity of ARC, WARC and/or GZip file(s)" );
			System.out.println( " -e   show errors" );
			System.out.println( " -d   decompress" );
			//System.out.println( " -1   compress faster" );
			//System.out.println( " -9   compress better" );
		}
		else {
			if ( arguments.idMap.containsKey( A_DECOMPRESS ) ) {
				new DecompressTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_COMPRESS ) ) {
				new CompressTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_TEST ) ) {
				new TestTask( arguments );
			}
		}
	}

	static void taskFileListFeeder(List<String> filesList, Task task) {
		String fileSeparator = System.getProperty( "file.separator" );
		File parentFile;
		String filepart;
		FilenameFilter filter;
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
				filter = new AcceptAllFilter();
			}
			else {
				filter = new AcceptAllFilter();
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

	static void taskFileFeeder(File parentFile, FilenameFilter filter, Task task) {
		if ( parentFile.isFile() ) {
			task.process( parentFile );
		}
		else if ( parentFile.isDirectory() ) {
			File[] files = parentFile.listFiles();
			for ( int i=0; i<files.length; ++i ) {
				if ( files[ i ].isFile() ) {
					task.process( files[ i ] );
				}
				else {
					taskFileFeeder( files[ i ], filter, task );
				}
			}
		}
	}

	static class AcceptAllFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return true;
		}
	}

}
