package org.jwat.tools.tasks.compress;

import java.io.File;
import java.util.zip.Deflater;

public class CompressionOptions {

	protected int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	protected boolean bBatch = false;

	protected boolean bDryrun = false;

	protected boolean bVerify = false;

	protected boolean bRemove = false;

	protected File dstPath;

	protected File lstFile;

}
