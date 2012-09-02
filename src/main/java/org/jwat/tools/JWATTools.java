package org.jwat.tools;

import org.jwat.tools.tasks.CompressTask;
import org.jwat.tools.tasks.ConvertTask;
import org.jwat.tools.tasks.DecompressTask;
import org.jwat.tools.tasks.IntervalTask;
import org.jwat.tools.tasks.UnpackTask;
import org.jwat.tools.tasks.test.TestTask;

public class JWATTools {

	public static final int A_DECOMPRESS = 1;
	public static final int A_COMPRESS = 2;
	public static final int A_FILES = 3;
	public static final int A_TEST = 4;
	public static final int A_SHOW_ERRORS = 5;
	public static final int A_RECURSIVE = 6;
	public static final int A_WORKERS = 6;
	public static final int A_INTERVAL = 7;
	public static final int A_UNPACK = 8;
	public static final int A_CONVERT = 9;

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
		cmdLine.addOption( "-r", A_RECURSIVE );
		cmdLine.addOption( "-w=", A_WORKERS );
		cmdLine.addOption( "--test", A_TEST );
		cmdLine.addOption( "-i", A_INTERVAL );
		cmdLine.addOption( "-u", A_UNPACK );
		cmdLine.addOption( "-c", A_CONVERT );
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
			System.out.println( "JWATTools v0.5.2" );
			/*
			System.out.println( "usage: JWATTools [-dt19] [file ...]" );
			System.out.println( " -t --test        test compressed file integrity" );
			System.out.println( " -d --decompress  decompress" );
			System.out.println( " -1 --fast        compress faster" );
			System.out.println( " -9 --best        compress better" );
			*/
			System.out.println( "usage: JWATTools [-dte19] [file ...]" );
			System.out.println( " -t   test validity of ARC, WARC and/or GZip file(s)" );
			System.out.println( " -e   show errors" );
			System.out.println( " -d   decompress" );
			System.out.println( " -r   recursive" );
			System.out.println( " -1   compress faster" );
			System.out.println( " -9   compress better" );
			System.out.println( " -i   interval extract" );
			System.out.println( " -u   unpack multifile gzip" );
			System.out.println( " -c   convert arc to warc" );
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
			else if ( arguments.idMap.containsKey( A_INTERVAL ) ) {
				new IntervalTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_UNPACK ) ) {
				new UnpackTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_CONVERT ) ) {
				new ConvertTask( arguments );
			}
		}
	}

}
