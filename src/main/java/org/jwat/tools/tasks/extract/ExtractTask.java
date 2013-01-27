package org.jwat.tools.tasks.extract;

import java.io.File;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.CommandLine.Arguments;
import org.jwat.tools.core.Task;

public class ExtractTask extends Task {

	@Override
	public void command(Arguments arguments) {
		CommandLine.Argument argument;
		// Thread workers.
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		threadpool_feeder_lifecycle( filesList, this );
	}

	@Override
	public void process(File file) {
		ExtractFile cdxFile = new ExtractFile();
		cdxFile.processFile(file);
	}

}
