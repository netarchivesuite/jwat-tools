package org.jwat.tools.tasks.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jwat.common.SecurityProviderTools;
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
		System.out.println("FileTools v" + JWATTools.getVersionString());
		System.out.println("jwattools [-o<file>] digest <filepattern>... ");
		System.out.println("");
		System.out.println("Digest file(s)");
		System.out.println("Use of this is mostly for debugging purposes.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(getMessageDigestAlgos());
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
			/*
			cliOptions.addOption(null, "--base16", A_BASE16, 0, null);
			cliOptions.addOption(null, "--base32", A_BASE32, 0, null);
			cliOptions.addOption(null, "--base64", A_BASE64, 0, null);
			*/
			cliOptions.addOption("-a", "--digest-algorithm", A_DIGEST_ALGO, 0, null).setValueRequired();
			cmdLine = ArgumentParser.parse(cmdLine.argsArray, cliOptions, cmdLine);
		}
		catch (ArgumentParserException e) {
			System.out.println( DigestTaskCLI.class.getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}

		DigestOptions options = new DigestOptions();

		Argument argument;
		String tmpStr;

		// Files
		argument = cmdLine.idMap.get( JWATTools.A_FILES );
		options.filesList = new LinkedList<String>();
		//options.filesList = argument.values;
		if (argument != null) {
			options.filesList.add( argument.value );
		}

		argument = cmdLine.idMap.get(A_DIGEST_ALGO);
		if (argument != null) {
			options.mdAlgo = argument.value;
			try {
				options.md = MessageDigest.getInstance(argument.value);
			}
			catch (NoSuchAlgorithmException e) {
				System.out.println("Unsupported digest algorithm: " + argument.value);
				System.exit(-1);
			}
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

	public static Map<String, Set<String>> digestAlgos = new TreeMap<>();
	public static Set<String> digestAliases = new TreeSet<>();
	public static Set<String> digestAndAliases = new TreeSet<>();

	public static String getMessageDigestAlgos() {
		final String digestClassName = MessageDigest.class.getSimpleName();
		final String aliasPrefix = "Alg.Alias." + digestClassName + ".";
		final int aliasPrefixLen = aliasPrefix.length();
		Provider[] providers = Security.getProviders();
		String providerAlias;
		if (SecurityProviderTools.isProviderAvailable(providers, "BC")) {
			providerAlias = "BC";
		}
		else {
			providerAlias = "SUN";
		}
		try {
			Provider provider = Security.getProvider(providerAlias);
			Set<Service> services = provider.getServices();
			services.stream().forEach(service -> {
				String algorithm;
				Set<String> aliases;
				if (digestClassName.equalsIgnoreCase(service.getType())) {
					algorithm = service.getAlgorithm();
					char[] charArr = algorithm.toCharArray();
					int charIdx = charArr.length - 1;
					char c;
					boolean b = true;
					while (b && charIdx >= 0) {
						c = charArr[charIdx--];
						b = ((c >= '0' && c<= '9') || c == '.');
					}
					if (charIdx != -1 && !(charIdx == 1 && algorithm.startsWith("OID."))) {
						aliases = digestAlgos.get(algorithm);
						if (aliases == null) {
							digestAlgos.put(algorithm, new TreeSet<String>());
							digestAndAliases.add(algorithm);
						}
					}
				}
			});
			provider.keySet().stream().map(Object::toString).filter(s -> s.startsWith(aliasPrefix)).forEach(s -> {
				String alias = s.substring(aliasPrefixLen);
				String algorithm = provider.get(s).toString();
				if (alias.compareToIgnoreCase(algorithm) != 0) {
					char[] charArr = alias.toCharArray();
					int charIdx = charArr.length - 1;
					char c;
					boolean b = true;
					while (b && charIdx >= 0) {
						c = charArr[charIdx--];
						b = ((c >= '0' && c<= '9') || c == '.');
					}
					if (charIdx != -1 && !(charIdx == 1 && alias.startsWith("OID."))) {
						Set<String> algorithms = digestAlgos.get(algorithm);
						if (algorithms != null) {
							algorithms.add(alias);
							digestAliases.add(alias);
							digestAndAliases.add(alias);
						}
					}
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		final StringBuffer sb = new StringBuffer();
		//String prefix = null;
		//int prefixLen = 0;
		digestAlgos.entrySet().forEach(e -> {
			Set<String> algorithms = e.getValue();
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(e.getKey());
			Iterator<String> aliasIter = algorithms.iterator();
			if (aliasIter.hasNext()) {
				sb.append(" (");
				sb.append(aliasIter.next());
				while (aliasIter.hasNext()) {
					sb.append(", ");
					sb.append(aliasIter.next());
				}
				sb.append(")");
			}
		});
		return sb.toString();
	}

}
