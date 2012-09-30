package org.jwat.tools.tasks;

import java.io.File;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.Task;

public class CDXTask extends Task {

	public CDXTask() {
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		String srcFname = srcFile.getName();

		System.out.println( "CDX!" );
		// CDX b e a m s c v n g
		/*
		date
		ip
		url
		mimetype
		response code
		old stylechecksum
		v uncompressed arc file offset * 
		n arc document length * 
		g file name 
		*/
	}

}
