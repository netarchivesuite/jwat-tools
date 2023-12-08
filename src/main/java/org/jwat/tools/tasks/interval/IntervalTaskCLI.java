package org.jwat.tools.tasks.interval;

import java.util.LinkedList;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class IntervalTaskCLI extends TaskCLI {

	public static final String commandName = "interval";

	public static final String commandDescription = "interval extract";

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools [-o<file>] interval offset1 offset2 srcfile dstfile");
		System.out.println("");
		System.out.println("Extract the byte interval from offset1 to offset2 from a file.");
		System.out.println("Offset1/2 can be decimal or hexadecimal ($<x> or 0x<x>).");
		System.out.println("Offset2 can also be a length-offset (+<x> +$<x> +0x<x>).");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println("none");
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		IntervalTask task = new IntervalTask();
		IntervalOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static final int A_OFFSET1 = 101;
	public static final int A_OFFSET2 = 102;
	public static final int A_DSTFILE = 103;

	public static IntervalOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument( "offset1", A_OFFSET1, 1, 1);
			cliOptions.addNamedArgument( "offset2", A_OFFSET2, 1, 1);
			cliOptions.addNamedArgument( "files", JWATTools.A_FILES, 1, 1 );
			cliOptions.addNamedArgument( "dstfile", A_DSTFILE, 1, 1);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( IntervalTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		IntervalOptions options = new IntervalOptions();

		Argument argument;
		String tmpStr;

		argument = cmdLine.idMap.get( A_OFFSET1 );
		tmpStr = argument.value.toLowerCase();
		try {
			if (tmpStr.startsWith("$")) {
				options.sIdx = Long.parseLong(tmpStr.substring(1), 16);
			}
			else if (tmpStr.startsWith("0x")) {
				options.sIdx = Long.parseLong(tmpStr.substring(2), 16);
			}
			else {
				options.sIdx = Long.parseLong(tmpStr);
			}
		}
		catch (NumberFormatException e) {
			System.out.println("Incorrect sidx!");
			System.exit(1);
		}

		argument = cmdLine.idMap.get( A_OFFSET2 );
		tmpStr = argument.value.toLowerCase();
		options.bPlusEIdx = tmpStr.startsWith("+");
		if (options.bPlusEIdx) {
			tmpStr = tmpStr.substring(1);
		}
		try {
			if (tmpStr.startsWith("$")) {
				options.eIdx = Long.parseLong(tmpStr.substring(1), 16);
			}
			else if (tmpStr.startsWith("0x")) {
				options.eIdx = Long.parseLong(tmpStr.substring(2), 16);
			}
			else {
				options.eIdx = Long.parseLong(tmpStr);
			}
			if (options.bPlusEIdx) {
				options.eIdx += options.sIdx;
			}
		}
		catch (NumberFormatException e) {
			System.out.println("Incorrect sidx!");
			System.exit(1);
		}

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = new LinkedList<String>();
		//options.filesList = argument.values;
		options.filesList.add( argument.value );

		argument = cmdLine.idMap.get( A_DSTFILE );
		options.dstName = argument.value;

		return options;
	}

}
