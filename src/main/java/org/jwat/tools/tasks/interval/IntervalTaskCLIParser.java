package org.jwat.tools.tasks.interval;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class IntervalTaskCLIParser {

	protected IntervalTaskCLIParser() {
	}

	public static IntervalOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
		try {
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParseException e) {
			System.out.println( IntervalTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		IntervalOptions options = new IntervalOptions();

		Argument argument;
		String tmpStr;

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		/*
		System.out.println(filesList.size());
		for (int i=0; i<filesList.size(); ++i) {
			System.out.println(filesList.get(i));
		}
		*/

		if (options.filesList.size() == 4) {
			tmpStr = options.filesList.remove(0).toLowerCase();
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
			tmpStr = options.filesList.remove(0).toLowerCase();
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
			options.dstName = options.filesList.remove(1);
		}
		else {
			System.out.println("Incorrect arguments!");
		}

		return options;
	}

}
