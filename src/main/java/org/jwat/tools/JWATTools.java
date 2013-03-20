package org.jwat.tools;

import java.util.HashMap;
import java.util.Map;

import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Argument;
import org.jwat.tools.tasks.Task;
import org.jwat.tools.tasks.UnpackTask;
import org.jwat.tools.tasks.arc2warc.Arc2WarcTask;
import org.jwat.tools.tasks.cdx.CDXTask;
import org.jwat.tools.tasks.compress.CompressTask;
import org.jwat.tools.tasks.decompress.DecompressTask;
import org.jwat.tools.tasks.extract.ExtractTask;
import org.jwat.tools.tasks.interval.IntervalTask;
import org.jwat.tools.tasks.pathindex.PathIndexTask;
import org.jwat.tools.tasks.test.TestTask;

public class JWATTools {

	public static final int A_COMMAND = 1;
	public static final int A_FILES = 2;
	public static final int A_WORKERS = 3;
	public static final int A_COMPRESS = 4;
	public static final int A_SHOW_ERRORS = 5;
	public static final int A_RECURSIVE = 6;
	public static final int A_XML = 7;
	public static final int A_LAX = 8;
	public static final int A_OUTPUT = 9;
	public static final int A_DEST = 10;
	public static final int A_OVERWRITE = 11;
	public static final int A_PREFIX = 12;
	public static final int A_IGNORE_DIGEST = 13;
	public static final int A_BAD = 14;
	public static final int A_AFTER = 15;
	public static final int A_TARGET_URI = 16;

	public static void main(String[] args) {
		JWATTools tools = new JWATTools();
		tools.Main( args );
	}

	public class Command {
		Class<? extends Task> task;
	}

	public static Map<String, Class<? extends Task>> commands = new HashMap<String, Class<? extends Task>>();

	public static CommandLine cmdLine = new CommandLine();

	public void configure_cli() {
		commands.put("help", HelpTask.class);
		commands.put("arc2warc", Arc2WarcTask.class);
		commands.put("cdx", CDXTask.class);
		commands.put("compress", DecompressTask.class);
		commands.put("decompress", CompressTask.class);
		commands.put("extract", ExtractTask.class);
		commands.put("interval", IntervalTask.class);
		commands.put("pathindex", PathIndexTask.class);
		commands.put("test", TestTask.class);
		commands.put("unpack", UnpackTask.class);

		cmdLine.addListArgument( "command", A_COMMAND, 1, 1);
		cmdLine.addOption("-1", A_COMPRESS, 1);
		cmdLine.addOption("-2", A_COMPRESS, 2);
		cmdLine.addOption("-3", A_COMPRESS, 3);
		cmdLine.addOption("-4", A_COMPRESS, 4);
		cmdLine.addOption("-5", A_COMPRESS, 5);
		cmdLine.addOption("-6", A_COMPRESS, 6);
		cmdLine.addOption("-7", A_COMPRESS, 7);
		cmdLine.addOption("-8", A_COMPRESS, 8);
		cmdLine.addOption("-9", A_COMPRESS, 9);
		cmdLine.addOption("--fast", A_COMPRESS, 1);
		cmdLine.addOption("--best", A_COMPRESS, 9);
		cmdLine.addOption("-e", A_SHOW_ERRORS);
		cmdLine.addOption("-l", A_LAX);
		cmdLine.addOption("-r", A_RECURSIVE);
		cmdLine.addOption("-w=", A_WORKERS);
		cmdLine.addOption("-x", A_XML);
		cmdLine.addOption("-o=", A_OUTPUT);
		cmdLine.addOption("-d=", A_DEST);
		cmdLine.addOption("--overwrite", A_OVERWRITE);
		cmdLine.addOption("--prefix=", A_PREFIX);
		cmdLine.addOption("-i", A_IGNORE_DIGEST);
		cmdLine.addOption("--ignore-digest", A_IGNORE_DIGEST);
		cmdLine.addOption("-b", A_BAD);
		cmdLine.addOption("-a=", A_AFTER);
		cmdLine.addOption("-t=", A_TARGET_URI);
		cmdLine.addListArgument("files", A_FILES, 1, Integer.MAX_VALUE);
	}

	public void show_help() {
		System.out.println("JWATTools v0.5.6");
		System.out.println("usage: JWATTools <command> [<args>]");
		System.out.println("");
		System.out.println("Commands:");
		System.out.println("   arc2warc     convert ARC to WARC");
		System.out.println("   cdx          create a CDX index (unsorted)");
		System.out.println("   compress     compress");
		System.out.println("   decompress   decompress");
		System.out.println("   extract      extract ARC/WARC record(s)");
		System.out.println("   interval     interval extract");
		System.out.println("   pathindex    create a heritrix path index (unsorted)");
		System.out.println("   test         test validity of ARC/WARC/GZip file(s)");
		System.out.println("   unpack       unpack multifile GZip");
		System.out.println("" );
		/*
		System.out.println("Options:");
		System.out.println("   -r      recursive (currently has no effect)");
		System.out.println("   -w<x>   set the amount of worker thread(s) (defaults to 1)");
		System.out.println("");
		*/
		System.out.println("See 'jwattools help <command>' for more information on a specific command.");
	}

	public void Main(String[] args) {
		configure_cli();
		CommandLine.Arguments arguments = null;
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
			show_help();
		}
		else {
			Argument argument = arguments.idMap.get( JWATTools.A_COMMAND );
			String commandStr = argument.value.toLowerCase();

			Class<? extends Task> clazz = commands.get(commandStr);
			if (clazz != null) {
				try {
					Task task = clazz.newInstance();
					task.command(arguments);
				}
				catch (InstantiationException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Unknown command -- " + commandStr);
			}
		}
	}

}
