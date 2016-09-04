package org.jwat.tools.tasks.compress;

import java.io.File;
import java.util.List;
import java.util.zip.Deflater;

public class CompressOptions {

	/** Threads to use in thread pool. */
	public int threads = 1;

	public int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	public boolean bBatch = false;

	public boolean bDryrun = false;

	public boolean bVerify = false;

	public boolean bRemove = false;

	public File dstPath;

	public File lstFile;

	public List<String> filesList;

}
