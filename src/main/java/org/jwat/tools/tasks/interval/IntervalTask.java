package org.jwat.tools.tasks.interval;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.jwat.tools.tasks.AbstractTask;

public class IntervalTask extends AbstractTask {

	private IntervalOptions options;

	public IntervalTask() {
	}

	public void runtask(IntervalOptions options) {
		this.options = options;
		filelist_feeder( options.filesList, this );
	}

	@Override
	public void process(File srcFile) {
		RandomAccessFile raf = null;
		OutputStream out = null;
		byte[] buffer = new byte[8192];
		try {
			out = new BufferedOutputStream(new FileOutputStream(options.dstName, false), 8192);
			raf = new RandomAccessFile( srcFile, "r" );
			raf.seek(options.sIdx);
			long remaining = options.eIdx - options.sIdx;
			int read = 0;
			System.out.println(options.sIdx);
			System.out.println(options.eIdx);
			while (remaining > 0 && read != -1) {
				read = Math.min(buffer.length, (int)Math.min(Integer.MAX_VALUE, remaining));
				read = raf.read(buffer, 0, read);
				if (read > 0) {
					remaining -= read;
					out.write(buffer, 0, read);
				}
			}
		}
		catch (IOException e) {
		}
		finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
					out = null;
				}
				catch (IOException e) {
				}
			}
			if (raf != null) {
				try {
					raf.close();
					raf = null;
				}
				catch (IOException e) {
				}
			}
		}
	}

}
