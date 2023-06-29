package org.jwat.tools.tasks.unchunk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jwat.common.DigestInputStreamChunkedNoSkip;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.tools.tasks.AbstractTask;

public class UnchunkTask extends AbstractTask {

	private UnchunkOptions options;

	public UnchunkTask() {
	}

	public void runtask(UnchunkOptions options) {
		this.options = options;
		filelist_feeder( options.filesList, this );
	}

	@Override
	public void process(File srcFile) {
		RandomAccessFile rafin = null;
		RandomAccessFileInputStream rafis = null;
		DigestInputStreamChunkedNoSkip dis = null;
		RandomAccessFile rafout = null;
		RandomAccessFileOutputStream rafos = null;
		try {
			rafin = new RandomAccessFile(srcFile, "r");
			rafis = new RandomAccessFileInputStream(rafin);
			rafout = new RandomAccessFile(srcFile + ".unchunked", "rw");
			rafos = new RandomAccessFileOutputStream(rafout);
			dis = new DigestInputStreamChunkedNoSkip(rafis, null, null, rafos);
			long remaining = rafin.length();
			long skipped;
			while (remaining > 0) {
				skipped = dis.skip(remaining);
				if (skipped > 0) {
					remaining -= skipped;
				}
			}
			dis.close();
			rafos.close();
			rafis.close();
			rafout.close();
			rafin.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}