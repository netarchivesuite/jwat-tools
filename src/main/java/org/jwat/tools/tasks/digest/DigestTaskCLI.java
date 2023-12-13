package org.jwat.tools.tasks.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import org.jwat.common.SecurityProviderAlgorithms;
import org.jwat.tools.JWATTools;
import org.jwat.tools.tasks.TaskCLI;

import com.antiaction.common.cli.Argument;
import com.antiaction.common.cli.ArgumentParser;
import com.antiaction.common.cli.ArgumentParserException;
import com.antiaction.common.cli.CommandLine;
import com.antiaction.common.cli.Options;

// TODO Fix common-cli to handle numerics in longName.
public class DigestTaskCLI extends TaskCLI {

	public static final String commandName = "digest";

	public static final String commandDescription = "calculate the digest of file(s)";

	@Override
	public void show_help() {
		SecurityProviderAlgorithms spa = SecurityProviderAlgorithms.getInstanceFor(MessageDigest.class);
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools [-o<file>] digest <filepattern>... ");
		System.out.println("");
		System.out.println("Digest file(s)");
		System.out.println("Use of this is mostly for debugging purposes.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -a <algorithm[,...]>  specify one or more digest algorithm");
		System.out.println("");
		System.out.println("Available digest algorithms:");
		System.out.println("----------------------------");
		System.out.println(spa.getAlgorithmListGrouped());
		System.out.println("");
	}

	@Override
	public void runtask(CommandLine cmdLine) {
		DigestTask task = new DigestTask();
		DigestOptions options = parseArguments(cmdLine);
		System.out.println(options.toString());
		task.runtask(options);
	}

	public static final int A_BASE16 = 101;
	public static final int A_BASE32 = 102;
	public static final int A_BASE64 = 103;
	public static final int A_DIGEST_ALGO = 104;

	public static DigestOptions parseArguments(CommandLine cmdLine) {
		Options cliOptions = new Options();
		try {
			cliOptions.addNamedArgument( "files", JWATTools.A_FILES, 1, 1 );
			//cliOptions.addOption(null, "--base16", A_BASE16, 0, null);
			//cliOptions.addOption(null, "--base32", A_BASE32, 0, null);
			//cliOptions.addOption(null, "--base64", A_BASE64, 0, null);
			cliOptions.addOption("-a", "--digest-algorithm", A_DIGEST_ALGO, 0, null).setValueRequired();
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( DigestTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		DigestOptions options = new DigestOptions();

		Argument argument;
		String[] values;
		DigestAlgo digestAlgo;

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = new LinkedList<String>();
		//options.filesList = argument.values;
		if (argument != null) {
			options.filesList.add( argument.value );
		}

		argument = cmdLine.idMap.get(A_DIGEST_ALGO);
		if (argument != null) {
			values = argument.value.split(",");
			options.digestAlgos = new DigestAlgo[values.length];
			for (int i=0; i<values.length; ++i) {
				try {
					digestAlgo = new DigestAlgo();
					digestAlgo.mdAlgo = values[i];
					digestAlgo.md = MessageDigest.getInstance(values[i]);
					options.digestAlgos[i] = digestAlgo;
				}
				catch (NoSuchAlgorithmException e) {
					System.out.println("Unsupported digest algorithm: " + values[i]);
					System.exit(-1);
				}
			}
		}

		if (options.filesList.size() > 0 && options.digestAlgos == null) {
			System.out.println("Missing digest algorithm.");
			System.exit(-1);
		}

		options.bBase16 = (cmdLine.idMap.get(A_BASE16) != null);
		options.bBase32 = (cmdLine.idMap.get(A_BASE32) != null);
		options.bBase64 = (cmdLine.idMap.get(A_BASE64) != null);

		if (options.bBase32 == false && options.bBase64 == false) {
			options.bBase16 = true;
		}
		options.bBase32 = true;
		options.bBase64 = true;

		return options;
	}

}
