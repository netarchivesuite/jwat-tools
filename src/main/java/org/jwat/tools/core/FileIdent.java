package org.jwat.tools.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jwat.arc.ArcReaderFactory;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReaderFactory;

public final class FileIdent {

	public static final int FILEID_ERROR = -1;
	public static final int FILEID_UNKNOWN = 0;
	public static final int FILEID_GZIP = 1;
	public static final int FILEID_ARC = 2;
	public static final int FILEID_WARC = 3;
	public static final int FILEID_ARC_GZ = 4;
	public static final int FILEID_WARC_GZ = 5;

	public static int identFile(File file) {
		int fileId;
		String fname = file.getName().toLowerCase();
		if (fname.endsWith(".arc.gz")) {
			fileId = FILEID_ARC_GZ;
		} else if (fname.endsWith(".warc.gz")) {
			fileId = FILEID_WARC_GZ;
		} else if (fname.endsWith(".arc")) {
			fileId = FILEID_ARC;
		} else if (fname.endsWith(".warc")) {
			fileId = FILEID_WARC;
		} else if (fname.endsWith(".gz")) {
			fileId = FILEID_GZIP;
		} else {
			fileId = identFileMagic(file);
		}
		return fileId;
	}

	public static int identFileMagic(File file) {
		int fileId = FILEID_UNKNOWN;
		byte[] magicBytes = new byte[16];
		int magicLength;
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin;
		ByteCountingPushBackInputStream pbin = null;
		try {
			raf = new RandomAccessFile( file, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream(rafin, 16);
			magicLength = pbin.readFully(magicBytes);
			if (magicLength == 16) {
				if (GzipReader.isGzipped(pbin)) {
					fileId = FILEID_GZIP;
					// TODO check for compress arc or warc too
				} else if (ArcReaderFactory.isArcFile(pbin)) {
					fileId = FILEID_ARC;
				} else if (WarcReaderFactory.isWarcFile(pbin)) {
					fileId = FILEID_WARC;
				}
			}
		} catch (FileNotFoundException e) {
			fileId = FILEID_ERROR;
			System.out.println("Error reading: " + file.getPath());
		} catch (IOException e) {
			fileId = FILEID_ERROR;
			e.printStackTrace();
		}
		finally {
			if (pbin != null) {
				try {
					pbin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return fileId;
	}

}
