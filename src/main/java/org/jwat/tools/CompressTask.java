package org.jwat.tools;

import java.io.File;

public class CompressTask extends Task {

	public CompressTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_COMPRESS );
		int compressionLevel = argument.argDef.subId;
		System.out.println( "Compression level: " + compressionLevel );
		System.out.println( "Unsupported..." );
	}

	@Override
	public void process(File file) {
	}

}
