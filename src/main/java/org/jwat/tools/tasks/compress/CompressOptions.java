package org.jwat.tools.tasks.compress;

import java.io.File;
import java.util.List;
import java.util.zip.Deflater;

public class CompressOptions {

	public boolean bQueueFirst = false;

	/** Threads to use in thread pool. */
	public int threads = 1;

	public int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	public boolean bBatch = false;

	public boolean bDryrun = false;

	public boolean bVerify = false;

	public boolean bRemove = false;

	public boolean bTwopass = false;

	public boolean bHeaderFiles = false;

	public boolean bQuiet = false;

	public boolean bValidateDigest = true;

	public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;

	public File dstPath;

	public File lstFile;

	public File hdrFile;

	public File failFile;

	public List<String> filesList;

}
