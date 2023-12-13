package org.jwat.tools.tasks.unchunk;

import java.util.LinkedList;

import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class UnchunkTaskCLI extends TaskCLI {

	public static final String commandName = "unchunk";

	public static final String commandDescription = "unchunk file(s) containing only chunked transfter encoded data";

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools [-o<file>] unchunk <filepattern>...");
		System.out.println("");
		System.out.println("Unchunk one or more files which consist of chunked transfer encoded data files.");
		System.out.println("This command only handles the chunked data.");
		System.out.println("All encapsulating headers or similar must be removed before using this command.");
		System.out.println("This includes removing headers for ARC, WARC, HTTP etc.");
		System.out.println("Use of this is mostly for debugging purposes.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println("none");
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		UnchunkTask task = new UnchunkTask();
		UnchunkOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static UnchunkOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument( "files", JWATTools.A_FILES, 1, 1 );
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( UnchunkTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		UnchunkOptions options = new UnchunkOptions();

		Argument argument;

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = new LinkedList<String>();
		//options.filesList = argument.values;
		options.filesList.add( argument.value );

		return options;
	}

}
