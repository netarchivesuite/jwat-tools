package org.jwat.tools.tasks.pathindex;

import java.io.File;
import java.util.List;

import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.SynchronizedOutput;
import org.jwat.tools.tasks.ProcessTask;

public class PathIndexTask extends ProcessTask {

	/** Output stream. */
	private SynchronizedOutput pathIndexOutput;

	public PathIndexTask() {
	}

	@Override
	public void show_help() {
		System.out.println("jwattools pathindex [-o OUTPUT_FILE] <paths>");
		System.out.println("");
		System.out.println("create a pathindex from one or more ARC/WARC files");
		System.out.println("");
		System.out.println("\tRead through ARC/WARC file(s) and create a pathindex file.");
		System.out.println("");
		System.out.println("options:");
		System.out.println("");
		System.out.println(" -o<file>  output cdx filename (unsorted)");
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		File outputFile = new File("path-index.unsorted.out");
		argument = arguments.idMap.get( JWATTools.A_OUTPUT );
		if ( argument != null && argument.value != null ) {
			outputFile = new File(argument.value);
			if (outputFile.isDirectory()) {
				System.out.println("Can not output to a directory!");
				System.exit(1);
			} else if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
				if (!outputFile.getParentFile().mkdirs()) {
					System.out.println("Could not create parent directories!");
					System.exit(1);
				}
			}
		}

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		pathIndexOutput = new SynchronizedOutput(outputFile);

		filelist_feeder(filesList, this);

		pathIndexOutput.out.flush();
		pathIndexOutput.out.close();
	}

	@Override
	public void process(File srcFile) {
		StringBuilder sb = new StringBuilder();
		FileIdent fileIdent = FileIdent.ident(srcFile);
		if (srcFile.length() > 0) {
			// debug
			//System.out.println(fileIdent.filenameId + " " + fileIdent.streamId + " " + srcFile.getName());
			if (fileIdent.filenameId != fileIdent.streamId) {
				cout.println("Wrong extension: '" + srcFile.getPath() + "'");
			}
			switch (fileIdent.streamId) {
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
				break;
			}
		} else {
			switch (fileIdent.filenameId) {
			case FileIdent.FILEID_ARC:
			case FileIdent.FILEID_WARC:
			case FileIdent.FILEID_ARC_GZ:
			case FileIdent.FILEID_WARC_GZ:
				cout.println("Empty file: '" + srcFile.getPath() + "'");
				break;
			default:
				break;
			}
		}
	}

}
