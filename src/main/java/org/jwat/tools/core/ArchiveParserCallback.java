package org.jwat.tools.core;

import java.io.File;
import java.io.IOException;

import org.jwat.arc.ArcRecordBase;
import org.jwat.gzip.GzipEntry;
import org.jwat.warc.WarcRecord;

public interface ArchiveParserCallback {

	public void apcFileId(File file, int fileId);

	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset);

	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException;

	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException;

	public void apcUpdateConsumed(long consumed);

	public void apcRuntimeError(Throwable t, long offset, long consumed);

	public void apcDone();

}
