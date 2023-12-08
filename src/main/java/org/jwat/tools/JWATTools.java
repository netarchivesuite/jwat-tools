package org.jwat.tools;

import java.lang.reflect.Field;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jwat.common.SecurityProviderTools;
import org.jwat.tools.tasks.TaskCLI;
import org.jwat.tools.tasks.arc2warc.Arc2WarcTaskCLI;
import org.jwat.tools.tasks.cdx.CDXTaskCLI;
import org.jwat.tools.tasks.changed.ChangedTaskCLI;
import org.jwat.tools.tasks.compress.CompressTaskCLI;
import org.jwat.tools.tasks.containermd.ContainerMDTaskCLI;
import org.jwat.tools.tasks.decompress.DecompressTaskCLI;
import org.jwat.tools.tasks.delete.DeleteTaskCLI;
import org.jwat.tools.tasks.digest.DigestTaskCLI;
import org.jwat.tools.tasks.extract.ExtractTaskCLI;
import org.jwat.tools.tasks.headers2cdx.Headers2CDXTaskCLI;
import org.jwat.tools.tasks.interval.IntervalTaskCLI;
import org.jwat.tools.tasks.pathindex.PathIndexTaskCLI;
import org.jwat.tools.tasks.test.TestTaskCLI;
import org.jwat.tools.tasks.unchunk.UnchunkTaskCLI;
import org.jwat.tools.tasks.unpack.UnpackTaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class JWATTools {

	public static final int A_WORKERS = 1;
	public static final int A_QUEUE_FIRST = 2;
	public static final int A_QUIET = 3;
	public static final int A_COMMAND = 4;
	public static final int A_FILES = 5;

	public static void main(String[] args) {
		JWATTools tools = new JWATTools();
		tools.Main( args );
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
	public static void configure_cli() {
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
				Headers2CDXTaskCLI.class,
				DigestTaskCLI.class,
				UnchunkTaskCLI.class
		};
		addCommands((Class<? extends TaskCLI>[])tasks);
	}

	public static void show_commands() {
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
		System.out.println("See 'jwattools help <command>' for more information on a specific command.");
		System.out.println("");
	}

	public static String getVersionString() {
		Package pkg = Package.getPackage("org.jwat.tools");
		String version = null;
		if (pkg != null) {
			version = pkg.getSpecificationVersion();
		}
		if (version == null) {
			version = "N/A";
		}
		return version;
	}

	public static void show_help() {
		System.out.println("JWATTools v" + getVersionString());
		//System.out.println(getVersionString("org.jwat.common"));
		//System.out.println(getVersionString("org.jwat.gzip"));
		//System.out.println(getVersionString("org.jwat.arc"));
		//System.out.println(getVersionString("org.jwat.warc"));
		System.out.println("usage: JWATTools <command> [<args>]");
		System.out.println("");
		show_commands();
	}

	public void Main(String[] args) {
		Provider[] providers = SecurityProviderTools.getSecurityProviders();
		if (!SecurityProviderTools.isProviderAvailable(providers, "BC")) {
			SecurityProviderTools.loadBCProvider();
		}
		CommandLine cmdLine = null;
		configure_cli();
		try {
			options.addOption(null, "--queue-first", A_QUEUE_FIRST, 0, null);
			options.addOption("-w", "--workers", A_WORKERS, 0, null).setValueRequired();
			options.addNamedArgument( "command", A_COMMAND, 0, 1).setStopParsing();
			cmdLine = ArgumentParser.parse(args, options, null);
		}
		catch (ArgumentParserException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}
		Argument argument = cmdLine.idMap.get( JWATTools.A_COMMAND );
		if ( argument == null ) {
			show_help();
		}
		else {
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
