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
import com.antiaction.common.cli.CommandLine;

public class TestTaskCLI extends TaskCLI {

	public static final String commandName = "test";

	public static final String commandDescription = "test validity of ARC/WARC/GZip file(s)";

	@Override
	public void show_help() {
		System.out.println("jwattools test [-beilx] [-w THREADS] [-a<yyyyMMddHHmmss>] <filepattern>...");
		System.out.println("");
		System.out.println("test one or more ARC/WARC/GZip files");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -a<yyyyMMddHHmmss>  only test files with last-modified after <yyyyMMddHHmmss>");
		System.out.println(" -b                  tag/rename files with errors/warnings (*.bad)");
		System.out.println(" -e                  show errors");
		System.out.println(" -i --ignore-digest  skip digest calculation and validation");
		System.out.println(" -l                  relaxed URL URI validation");
		System.out.println(" -x                  to validate text/xml payload (eg. mets)");
		System.out.println(" -w<x>               set the amount of worker thread(s) (defaults to 1)");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
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
		if ( cmdLine.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			options.bShowErrors = true;
		}
		System.out.println("Showing errors: " + options.bShowErrors);

		// Ignore digest.
		if ( cmdLine.idMap.containsKey( JWATTools.A_IGNORE_DIGEST ) ) {
			options.bValidateDigest = false;
		}
		System.out.println("Validate digest: " + options.bValidateDigest);

		// Relaxed URI validation.
		if ( cmdLine.idMap.containsKey( JWATTools.A_LAX ) ) {
			options.uriProfile = UriProfile.RFC3986_ABS_16BIT_LAX;
			System.out.println("Using relaxed URI validation for ARC URL and WARC Target-URI.");
		}

		// XML validation.
		if ( cmdLine.idMap.containsKey( JWATTools.A_XML ) ) {
			options.validatorPlugins.add(new XmlValidatorPlugin());
		}

		// Tag.
		if ( cmdLine.idMap.containsKey( JWATTools.A_BAD ) ) {
			options.bBad = true;
			System.out.println("Tagging enabled for invalid files");
		}

		// After.
		argument = cmdLine.idMap.get( JWATTools.A_AFTER );
		if ( argument != null && argument.value != null ) {
			try {
				DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		        dateFormat.setLenient(false);
		        Date afterDate = dateFormat.parse(argument.value);
		        options.after = afterDate.getTime();
			} catch (ParseException e) {
				System.out.println("Invalid date format - " + argument.value);
			}
		}

        // Files.
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = argument.values;

		TestTask task = new TestTask();
		task.runtask(options);
	}

}
