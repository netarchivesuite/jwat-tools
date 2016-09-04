package org.jwat.tools.tasks.pathindex;

import java.io.File;

import org.jwat.archive.FileIdent;
import org.jwat.tools.tasks.ProcessTask;

import com.antiaction.common.cli.SynchronizedOutput;

public class PathIndexTask extends ProcessTask {

	/** Output stream. */
	private SynchronizedOutput pathIndexOutput;

	public PathIndexTask() {
	}

	public void runtask(PathIndexOptions options) {
		pathIndexOutput = new SynchronizedOutput(options.outputFile);

		filelist_feeder(options.filesList, this);

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
