package org.jwat.tools.tasks.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jwat.common.UriProfile;
import org.jwat.tools.JWATTools;
import org.jwat.tools.validators.XmlValidatorPlugin;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

public class TestTaskCLIParser {

	public static final int A_AFTER = 101;
	public static final int A_BAD = 102;
	public static final int A_SHOW_ERRORS = 103;
	public static final int A_IGNORE_DIGEST = 104;
	public static final int A_LAX = 105;
	public static final int A_XML = 106;

	protected TestTaskCLIParser() {
	}

	public static TestOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addOption("-w", "--workers", JWATTools.A_WORKERS, 0, null).setValueRequired();
			cliOptions.addOption("-a", null, A_AFTER, 0, null).setValueRequired();
			cliOptions.addOption("-b", null, A_BAD, 0, null);
			cliOptions.addOption("-e", null, A_SHOW_ERRORS, 0, null);
			cliOptions.addOption("-i", "--ignore-digest", A_IGNORE_DIGEST, 0, null);
			cliOptions.addOption("-l", null, A_LAX, 0, null);
			cliOptions.addOption("-x", null, A_XML, 0, null);
			cliOptions.addNamedArgument("files", JWATTools.A_FILES, 1, Integer.MAX_VALUE);
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( TestTaskCLIParser.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		TestOptions options = new TestOptions();

		Argument argument;

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
