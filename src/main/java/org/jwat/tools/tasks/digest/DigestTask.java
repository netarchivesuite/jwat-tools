package org.jwat.tools.tasks.digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jwat.common.Base16;
import org.jwat.common.Base32;
import org.jwat.common.Base64;
import org.jwat.tools.tasks.AbstractTask;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

public class DigestTask extends AbstractTask {

	private DigestOptions options;

	public DigestTask() {
	}

	public void runtask(DigestOptions options) {
		this.options = options;
		if (options.filesList.size() > 0) {
			filelist_feeder( options.filesList, this );
		}
		else {
		}
	}

    private byte[] isBuffer = new byte[65536];

    private byte[] readBuffer = new byte[65536];

	@Override
	public void process(File srcFile) {
		byte[] digest;
		FastBufferedInputStream in = null;
		int read;
		try {
			in = new FastBufferedInputStream(new FileInputStream(srcFile), isBuffer);
			while ((read = in.read(readBuffer)) != -1) {
				options.md.update(readBuffer, 0, read);
			}
			digest = options.md.digest();
			if (options.bBase16) {
				System.out.println(options.mdAlgo + ":" + Base16.encodeArray(digest) + " (base16/hex)");
			}
			if (options.bBase32) {
				System.out.println(options.mdAlgo + ":" + Base32.encodeArray(digest) + " (base32)");
			}
			if (options.bBase64) {
				System.out.println(options.mdAlgo + ":" + Base64.encodeArray(digest) + " (base64)");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

}
