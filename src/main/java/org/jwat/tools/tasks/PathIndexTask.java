package org.jwat.tools.tasks;

import java.io.File;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.core.Task;

public class PathIndexTask extends Task {

	/** Output stream. */
	private SynchronizedOutput pathIndexOutput;

	public PathIndexTask() {
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		pathIndexOutput = new SynchronizedOutput("path-index.unsorted.txt");

		filelist_feeder(filesList, this);

	}

	@Override
	public void process(File srcFile) {
		StringBuilder sb = new StringBuilder();
		if (srcFile.length() > 0) {
			int fileId = FileIdent.identFile(srcFile);
			switch (fileId) {
			case FileIdent.FILEID_GZIP:
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC_GZ:
				sb.setLength(0);
				sb.append(srcFile.getName());
				sb.append('\t');
				sb.append(srcFile.getPath());
				pathIndexOutput.out.println(sb.toString());
				break;
			default:
				System.out.println("Skipping: " + srcFile.getName());
				break;
			}
		}
	}

}
