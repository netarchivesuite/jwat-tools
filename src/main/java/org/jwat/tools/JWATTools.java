package org.jwat.tools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Argument;
import org.jwat.tools.tasks.Task;
import org.jwat.tools.tasks.UnpackTask;
import org.jwat.tools.tasks.arc2warc.Arc2WarcTask;
import org.jwat.tools.tasks.cdx.CDXTask;
import org.jwat.tools.tasks.changed.ChangedTask;
import org.jwat.tools.tasks.compress.CompressTask;
import org.jwat.tools.tasks.containermd.ContainerMDTask;
import org.jwat.tools.tasks.decompress.DecompressTask;
import org.jwat.tools.tasks.delete.DeleteTask;
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
	public static final int A_TESTRUN = 17;
	public static final int A_QUIET = 18;
	public static final int A_BATCHMODE = 19;
	public static final int A_REMOVE = 20;
	public static final int A_VERIFY = 21;
	public static final int A_DRYRUN = 22;

	public static void main(String[] args) {
		JWATTools tools = new JWATTools();
		tools.Main( args );
	}

	public class Command {
		Class<? extends Task> task;
	}

	public static List<Class<? extends Task>> commandList = new ArrayList<Class<? extends Task>>();

	public static Map<String, Class<? extends Task>> commandMap = new HashMap<String, Class<? extends Task>>();

	public static CommandLine cmdLine = new CommandLine();

	public static int maxCommandNameLength = 0;

	public static void addCommands(Class<? extends Task>[] tasks) {
		try {
			for (int i=0; i<tasks.length; ++i) {
				Field commandNameField = tasks[i].getField("commandName");
				String commandName = (String)commandNameField.get(null);
				commandList.add(tasks[i]);
				commandMap.put(commandName, tasks[i]);
				if (commandName.length() > maxCommandNameLength) {
					maxCommandNameLength = commandName.length();
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void configure_cli() {
		Class<?>[] tasks = new Class<?>[] {
				HelpTask.class,
				Arc2WarcTask.class,
				CDXTask.class,
				ChangedTask.class,
				CompressTask.class,
				ContainerMDTask.class,
				DecompressTask.class,
				DeleteTask.class,
				ExtractTask.class,
				IntervalTask.class,
				PathIndexTask.class,
				TestTask.class,
				UnpackTask.class,
		};
		addCommands((Class<? extends Task>[])tasks);

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
		cmdLine.addOption("-u=", A_TARGET_URI);
		cmdLine.addOption("-t", A_TESTRUN);
		cmdLine.addOption("-q", A_QUIET);
		cmdLine.addOption("--batch", A_BATCHMODE);
		cmdLine.addOption("--remove", A_REMOVE);
		cmdLine.addOption("--verify", A_VERIFY);
		cmdLine.addOption("--dryrun", A_DRYRUN);
		cmdLine.addListArgument("files", A_FILES, 1, Integer.MAX_VALUE);
	}

	public void show_commands() {
		Collections.sort(commandList, new Comparator<Class<? extends Task>>() {
			@Override
			public int compare(Class<? extends Task> t1, Class<? extends Task> t2) {
				Field commandNameField;
				try {
					commandNameField = t1.getField("commandName");
					String n1 = (String)commandNameField.get(null);
					commandNameField = t2.getField("commandName");
					String n2 = (String)commandNameField.get(null);
					return n1.compareTo(n2);
				} catch (SecurityException e) {
				} catch (NoSuchFieldException e) {
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
				return 0;
			}
		});
		System.out.println("Commands:");
		Field commandNameField;
		Field commandDescriptionField;
		for (int i=0; i<commandList.size(); ++i) {
			try {
				commandNameField = commandList.get(i).getField("commandName");
				commandDescriptionField = commandList.get(i).getField("commandDescription");
				String commandName = (String)commandNameField.get(null) + "                        ";
				String commandDescription = (String)commandDescriptionField.get(null);
				System.out.println(String.format("   %s   %s", commandName.substring(0, maxCommandNameLength), commandDescription));
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		System.out.println("");
		/*
		System.out.println("Options:");
		System.out.println("   -r      recursive (currently has no effect)");
		System.out.println("   -w<x>   set the amount of worker thread(s) (defaults to 1)");
		System.out.println("");
		*/
		System.out.println("See 'jwattools help <command>' for more information on a specific command.");
	}

	public void show_help() {
		Package pkg = Package.getPackage("org.jwat.tools");
		String version = null;
		if (pkg != null) {
			version = pkg.getSpecificationVersion();
		}
		if (version == null) {
			version = "N/A";
		}
		System.out.println("JWATTools v" + version);
		System.out.println("usage: JWATTools <command> [<args>]");
		System.out.println("");
		show_commands();
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

			Class<? extends Task> clazz = commandMap.get(commandStr);
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
