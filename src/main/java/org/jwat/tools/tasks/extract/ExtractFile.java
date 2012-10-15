package org.jwat.tools.tasks.extract;

import java.io.IOException;

import org.jwat.arc.ArcRecordBase;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.warc.WarcRecord;

public class ExtractFile implements ArchiveParserCallback {

	@Override
	public void apcFileId(int fileId) {
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset,
			boolean compressed) throws IOException {
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset,
			boolean compressed) throws IOException {
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
	}

	@Override
	public void apcRuntimeError(Throwable t, long offset, long consumed) {
	}

}
