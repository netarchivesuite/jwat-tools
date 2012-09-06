package org.jwat.tools.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Semaphore;

import org.jwat.common.RandomAccessFileOutputStream;

public class SynchronizedOutput {

	private RandomAccessFile raf = null;
	private RandomAccessFileOutputStream rafOut = null;
	private Semaphore semaphore = new Semaphore(1);
	public PrintStream out = null;

	public SynchronizedOutput(String fname) {
		try {
			raf = new RandomAccessFile(fname, "rw");
			raf.seek(0);
			raf.setLength(0);
			RandomAccessFileOutputStream fout = new RandomAccessFileOutputStream(raf);
			out = new PrintStream(fout);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void acquired() {
		semaphore.acquireUninterruptibly();
	}

	public void release() {
		semaphore.release();
	}

	public void close() {
		semaphore = null;
		if (out != null) {
			out.flush();
			out.close();
			out = null;
		}
		if (rafOut != null) {
			try {
				rafOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			rafOut = null;
		}
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			raf = null;
		}
	}

}
