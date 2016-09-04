package org.jwat.tools.tasks.interval;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.CommandLine;

public class IntervalTaskCLI extends TaskCLI {

	public static final String commandName = "interval";

	public static final String commandDescription = "interval extract";

	@Override
	public void show_help() {
		System.out.println("jwattools [-o<file>] interval offset1 offset2 srcfile dstfile");
		System.out.println("");
		System.out.println("extract the byte interval from offset1 to offset2 from a file");
		System.out.println("");
		System.out.println("\tSkips data up to offset1 and save data to file until offset2 is reached.");
		System.out.println("\tOffset1/2 can be decimal or hexadecimal ($<x> or 0x<x>).");
		System.out.println("\tOffset2 can also be a length-ofsset (+<x>).");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output data filename");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
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

		IntervalTask task = new IntervalTask();
		task.runtask(options);
	}

}
