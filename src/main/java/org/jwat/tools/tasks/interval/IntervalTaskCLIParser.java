package org.jwat.tools.tasks.interval;

import java.util.LinkedList;

import org.jwat.tools.JWATTools;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParseException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class IntervalTaskCLIParser {

	public static final int A_OFFSET1 = 101;
	public static final int A_OFFSET2 = 102;
	public static final int A_DSTFILE = 103;

	protected IntervalTaskCLIParser() {
	}

	public static IntervalOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		cliOptions.addNamedArgument( "offset1", A_OFFSET1, 1, 1);
		cliOptions.addNamedArgument( "offset2", A_OFFSET2, 1, 1);
		cliOptions.addNamedArgument( "files", JWATTools.A_FILES, 1, 1 );
		cliOptions.addNamedArgument( "dstfile", A_DSTFILE, 1, 1);
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
