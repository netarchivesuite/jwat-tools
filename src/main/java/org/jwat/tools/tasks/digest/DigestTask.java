package org.jwat.tools.tasks.digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
		filelist_feeder( options.filesList, this );
	}

    private byte[] isBuffer = new byte[65536];

    private byte[] readBuffer = new byte[65536];

	@Override
	public void process(File srcFile) {
		MessageDigest md = null;
		byte[] digest;
		FastBufferedInputStream in = null;
		String digestAlgorithm = "SHA-1";
        try {
            md = MessageDigest.getInstance(digestAlgorithm);
        }
        catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
        	System.exit(-1);
        }
        int read;
        try {
			in = new FastBufferedInputStream(new FileInputStream(srcFile), isBuffer);
			while ((read = in.read(readBuffer)) != -1) {
				md.update(readBuffer, 0, read);
			}
			digest = md.digest();
			System.out.println(digestAlgorithm + ":" + Base16.encodeArray(digest) + " (base16/hex)");
			System.out.println(digestAlgorithm + ":" + Base32.encodeArray(digest) + " (base32)");
			System.out.println(digestAlgorithm + ":" + Base64.encodeArray(digest) + " (base64)");
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
