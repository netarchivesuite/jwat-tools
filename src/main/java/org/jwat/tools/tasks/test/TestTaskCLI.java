package org.jwat.tools.tasks.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jwat.common.UriProfile;
import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;
import org.jwat.tools.validators.XmlValidatorPlugin;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class TestTaskCLI extends TaskCLI {

	public static final String commandName = "test";

	public static final String commandDescription = "test validity of ARC/WARC/GZip file(s)";

	@Override
	public void show_help() {
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools test [-beilx] [-w THREADS] [-a<yyyyMMddHHmmss>] <filepattern>...");
		System.out.println("");
		System.out.println("Test one or more ARC/WARC/GZip files.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -a <yyyyMMddHHmmss>  only test files with last-modified after <yyyyMMddHHmmss>");
		System.out.println(" -b                   tag/rename files with errors/warnings (*.bad)");
		System.out.println(" -e                   show errors");
		System.out.println(" -h                   report HTTP header errors as ARC/WARC format errors");
		System.out.println(" -i --ignore-digest   skip digest calculation and validation");
		System.out.println(" -l                   relaxed URL URI validation");
		System.out.println(" -x                   to validate text/xml payload (eg. mets)");
		System.out.println("    --queue-first     queue files before processing");
		System.out.println(" -w <x>               set the amount of worker thread(s) (defaults to 1)");
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		TestTask task = new TestTask();
		TestOptions options = parseArguments(cmdLine);
		task.runtask(options);
	}

	public static final int A_AFTER = 101;
	public static final int A_BAD = 102;
	public static final int A_SHOW_ERRORS = 103;
	public static final int A_IGNORE_DIGEST = 104;
	public static final int A_HTTP_HEADER_ERRORS = 105;
	public static final int A_LAX = 106;
	public static final int A_XML = 107;

	public static TestOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption(null, "--queue-first", JWATTools.A_QUEUE_FIRST, 0, null);
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addOption("-a", null, A_AFTER, 0, null).setValueRequired();
			cliOptions.addOption("-b", null, A_BAD, 0, null);
			cliOptions.addOption("-e", null, A_SHOW_ERRORS, 0, null);
			cliOptions.addOption("-h", null, A_HTTP_HEADER_ERRORS, 0, null);
			cliOptions.addOption("-i", "--ignore-digest", A_IGNORE_DIGEST, 0, null);
			cliOptions.addOption("-l", null, A_LAX, 0, null);
			cliOptions.addOption("-x", null, A_XML, 0, null);
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println(TestTaskCLI.class.getName() + ": " + e.getMessage());
			System.exit(1);
		}

		TestOptions options = new TestOptions();

		Argument argument;

		// Queue first.
		options.bQueueFirst = cmdLine.idMap.containsKey(JWATTools.A_QUEUE_FIRST);
		options.bHttpHeaderErrors = cmdLine.idMap.containsKey(A_HTTP_HEADER_ERRORS);

		// Thread workers.
		argument = cmdLine.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				options.threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
				System.out.println( "Invalid number of threads requested: " + argument.value );
				System.exit( 1 );
			}
		}
		if ( options.threads < 1 ) {
			System.out.println( "Invalid number of threads requested: " + options.threads );
			System.exit( 1 );
		}

		// Show errors.
		if ( cmdLine.idMap.containsKey( A_SHOW_ERRORS ) ) {
			options.bShowErrors = true;
		}
		System.out.println("Showing errors: " + options.bShowErrors);

		// Ignore digest.
		if ( cmdLine.idMap.containsKey( A_IGNORE_DIGEST ) ) {
			options.bValidateDigest = false;
		}
		System.out.println("Validate digest: " + options.bValidateDigest);

		// Relaxed URI validation.
		if ( cmdLine.idMap.containsKey( A_LAX ) ) {
			options.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}

		// XML validation.
		if ( cmdLine.idMap.containsKey( A_XML ) ) {
			options.validatorPlugins.add(new XmlValidatorPlugin());
		}

		// Tag.
		if ( cmdLine.idMap.containsKey( A_BAD ) ) {
			options.bBad = true;
			System.out.println("Tagging enabled for invalid files");
		}

		// After.
		argument = cmdLine.idMap.get( A_AFTER );
		if ( argument != null && argument.value != null ) {
			try {
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
				dateFormat.setLenient(false);
				Date afterDate = dateFormat.parse(argument.value);
				options.after = afterDate.getTime();
			} catch (ParseException e) {
				System.out.println("Invalid date format - " + argument.value);
				System.exit( 1 );
			}
		}

		// Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		return options;
	}

}
