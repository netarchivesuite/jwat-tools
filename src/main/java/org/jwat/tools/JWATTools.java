package org.jwat.tools;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jwat.tools.tasks.TaskCLI;
import org.jwat.tools.tasks.Task;
import org.jwat.tools.tasks.UnpackTaskCLI;
import org.jwat.tools.tasks.arc2warc.Arc2WarcTaskCLI;
import org.jwat.tools.tasks.cdx.CDXTaskCLI;
import org.jwat.tools.tasks.changed.ChangedTaskCLI;
import org.jwat.tools.tasks.compress.CompressTaskCLI;
import org.jwat.tools.tasks.containermd.ContainerMDTaskCLI;
import org.jwat.tools.tasks.decompress.DecompressTaskCLI;
import org.jwat.tools.tasks.delete.DeleteTaskCLI;
import org.jwat.tools.tasks.extract.ExtractTaskCLI;
import org.jwat.tools.tasks.interval.IntervalTaskCLI;
import org.jwat.tools.tasks.pathindex.PathIndexTaskCLI;
import org.jwat.tools.tasks.test.TestTaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;
import com.antiaction.common.cli.ParseException;

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

	public static List<Class<? extends TaskCLI>> commandList = new ArrayList<Class<? extends TaskCLI>>();

	public static Map<String, Class<? extends TaskCLI>> commandMap = new HashMap<String, Class<? extends TaskCLI>>();

	public static Options options = new Options();

	public static int maxCommandNameLength = 0;

	public static void addCommands(Class<? extends TaskCLI>[] tasks) {
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

	@SuppressWarnings("unchecked")
	public void configure_cli() {
		Class<?>[] tasks = new Class<?>[] {
				HelpTaskCLI.class,
				Arc2WarcTaskCLI.class,
				CDXTaskCLI.class,
				ChangedTaskCLI.class,
				CompressTaskCLI.class,
				ContainerMDTaskCLI.class,
				DecompressTaskCLI.class,
				DeleteTaskCLI.class,
				ExtractTaskCLI.class,
				IntervalTaskCLI.class,
				PathIndexTaskCLI.class,
				TestTaskCLI.class,
				UnpackTaskCLI.class,
		};
		addCommands((Class<? extends TaskCLI>[])tasks);

		options.addOption("-1", "--fast", A_COMPRESS, 1, null);
		options.addOption("-2", null, A_COMPRESS, 2, null);
		options.addOption("-3", null, A_COMPRESS, 3, null);
		options.addOption("-4", null, A_COMPRESS, 4, null);
		options.addOption("-5", null, A_COMPRESS, 5, null);
		options.addOption("-6", null, A_COMPRESS, 6, null);
		options.addOption("-7", null, A_COMPRESS, 7, null);
		options.addOption("-8", null, A_COMPRESS, 8, null);
		options.addOption("-9", "--best", A_COMPRESS, 9, null);
		options.addOption("-e", null, A_SHOW_ERRORS, 0, null);
		options.addOption("-l", null, A_LAX, 0, null);
		options.addOption("-r", null, A_RECURSIVE, 0, null);
		options.addOption("-w", "--workers", A_WORKERS, 0, null).setValueRequired();
		options.addOption("-x", null, A_XML, 0, null);
		options.addOption("-o", null, A_OUTPUT, 0, null).setValueRequired();
		options.addOption("-d", null, A_DEST, 0, null).setValueRequired();
		options.addOption(null, "--overwrite", A_OVERWRITE, 0, null);
		options.addOption(null, "--prefix=", A_PREFIX, 0, null);
		options.addOption("-i", "--ignore-digest", A_IGNORE_DIGEST, 0, null);
		options.addOption("-b", null, A_BAD, 0, null);
		options.addOption("-a", null, A_AFTER, 0, null).setValueRequired();
		options.addOption("-u", null, A_TARGET_URI, 0, null).setValueRequired();
		options.addOption("-t", null, A_TESTRUN, 0, null);
		options.addOption("-q", null, A_QUIET, 0, null);
		options.addOption(null, "--batch", A_BATCHMODE, 0, null);
		options.addOption(null, "--remove", A_REMOVE, 0, null);
		options.addOption(null, "--verify", A_VERIFY, 0, null);
		options.addOption(null, "--dryrun", A_DRYRUN, 0, null);
		options.addNamedArgument( "command", A_COMMAND, 1, 1);
		options.addNamedArgument("files", A_FILES, 1, Integer.MAX_VALUE);
	}

	public void show_commands() {
		Collections.sort(commandList, new Comparator<Class<? extends TaskCLI>>() {
			@Override
			public int compare(Class<? extends TaskCLI> t1, Class<? extends TaskCLI> t2) {
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
		CommandLine cmdLine = null;
		try {
			cmdLine = ArgumentParser.parse(options, args);
			/*
			for ( int i=0; i<arguments.switchArgsList.size(); ++i) {
				argument = arguments.switchArgsList.get( i );
				System.out.println( argument.argDef.id + "," + argument.argDef.subId + "=" + argument.value );
			}
			*/
		}
		catch (ParseException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}
		if ( cmdLine == null ) {
			show_help();
		}
		else {
			Argument argument = cmdLine.idMap.get( JWATTools.A_COMMAND );
			String commandStr = argument.value.toLowerCase();

			Class<? extends TaskCLI> clazz = commandMap.get(commandStr);
			if (clazz != null) {
				try {
					TaskCLI taskcli = clazz.newInstance();
					taskcli.runtask(cmdLine);
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
