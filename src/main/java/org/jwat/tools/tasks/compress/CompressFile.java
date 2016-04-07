package org.jwat.tools.tasks.compress;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ArrayUtils;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.DigestInputStreamNoSkip;
import org.jwat.common.Payload;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.gzip.GzipConstants;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.gzip.GzipWriter;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class CompressFile {

	//private static final int INPUT_BUFFER_SIZE = 1024 * 1024;
	private static final int INPUT_BUFFER_SIZE = 16384;

	//private static final int GZIP_OUTPUT_BUFFER_SIZE = 1024 * 1024;
	private static final int GZIP_OUTPUT_BUFFER_SIZE = 16384;

	//private static final int BUFFER_SIZE = 65 * 1024;
	private static final int BUFFER_SIZE = 8192;

	/** Source <code>File</code> object. */
	protected File srcFile;

	protected int compressionLevel = 9;

	protected boolean bVerify = true;

	protected File dstFile;

    protected boolean bVerified = false;

    protected byte[] md5DigestBytesOrg;

    protected byte[] md5DigestBytesVerify;

    protected byte[] md5compDigestBytesVerify;

	/**
	 * Compress input file according to its type.
	 * ARC/WARC files are compressed one record at a time and concatenated into on multi-entry GZip file.
	 * Other files are compressed as one entry.
	 * @param srcFile
	 */
	protected void compressFile(File srcFile) {
		this.srcFile = srcFile;
		String srcFname = srcFile.getName();
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin;
		ByteCountingPushBackInputStream pbin = null;
		try {
			raf = new RandomAccessFile( srcFile, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, INPUT_BUFFER_SIZE ), 32 );
			if (!GzipReader.isGzipped(pbin)) {
				String dstFname = srcFname + ".gz";
				dstFile = new File( srcFile.getParentFile(), dstFname );
				if ( !dstFile.exists() ) {
					System.out.println( srcFname + " -> " + dstFname );
					if ( ArcReaderFactory.isArcFile( pbin ) ) {
						compressArcFile( pbin );
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
						compressWarcFile( pbin );
					} else {
						compressNormalFile( pbin );
					}
				}
				else {
					System.out.println( dstFile.getName() + " already exists, skipping." );
				}
			}
			else if ( !srcFname.toLowerCase().endsWith( ".gz" ) ) {
				System.out.println( "Invalid extension: " + srcFname );
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (pbin != null) {
				try {
					pbin.close();
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

	protected void compressNormalFile(InputStream in) {
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE );
	        writer.setCompressionLevel(compressionLevel);

	        entry = new GzipEntry();
	        entry.magic = GzipConstants.GZIP_MAGIC;
	        entry.cm = GzipConstants.CM_DEFLATE;
	        entry.flg = 0;
	        entry.mtime = System.currentTimeMillis() / 1000;
	        entry.xfl = 0;
	        entry.os = GzipConstants.OS_UNKNOWN;
	        writer.writeEntryHeader(entry);

	        cout = entry.getOutputStream();

	        while ((read = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
	        	cout.write(buffer, 0, read);
	        }

	        cout.close();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (cout != null) {
				try {
					cout.close();
				} catch (IOException e) {
				}
			}
			if (writer != null) {
		        try {
					writer.close();
				} catch (IOException e) {
				}
			}
			if (out != null) {
		        try {
					out.close();
				} catch (IOException e) {
				}
			}
			if (in != null) {
		        try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}

	// TODO
	protected void compressArcFile(InputStream in) {
        byte[] buffer = new byte[BUFFER_SIZE];
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
		ArcReader arcReader = null;
		ArcRecordBase arcRecord = null;
		Payload payload;
		int read;
		InputStream pin = null;
        MessageDigest md5 = null;
        MessageDigest md5comp = null;
        InputStream uncompressedFileIn = null;
        GzipReader reader = null;
        InputStream uncompressedEntryIn = null;
        bVerified = false;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(compressionLevel);
	        entry = null;

	        if (bVerify) {
		        md5 = MessageDigest.getInstance("MD5");
		        md5comp = MessageDigest.getInstance("MD5");
	        }

	        arcReader = ArcReaderFactory.getReaderUncompressed( in );
			arcReader.setBlockDigestEnabled( false );
			arcReader.setPayloadDigestEnabled( false );
			while ((arcRecord = arcReader.getNextRecord()) != null) {
		        entry = new GzipEntry();
		        entry.magic = GzipConstants.GZIP_MAGIC;
		        entry.cm = GzipConstants.CM_DEFLATE;
		        entry.flg = 0;
		        entry.mtime = System.currentTimeMillis() / 1000;
		        entry.xfl = 0;
		        entry.os = GzipConstants.OS_UNKNOWN;
		        writer.writeEntryHeader(entry);

		        cout = entry.getOutputStream();
		        cout.write(arcRecord.header.headerBytes);
		        if (bVerify) {
		        	md5.update(arcRecord.header.headerBytes);
		        }

				payload = arcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	cout.write(buffer, 0, read);
			        	if (bVerify) {
				        	md5.update(buffer, 0, read);
			        	}
			        }
				}

				cout.close();
				cout = null;

		        arcRecord.close();
		        arcRecord = null;

			}
			writer.close();
			writer = null;

			if (bVerify) {
		        md5DigestBytesOrg = md5.digest();

		        md5.reset();

		        uncompressedFileIn = new FileInputStream(dstFile);
		        uncompressedFileIn = new DigestInputStreamNoSkip(uncompressedFileIn, md5comp);
		        reader = new GzipReader(uncompressedFileIn, GZIP_OUTPUT_BUFFER_SIZE);
		        while ((entry = reader.getNextEntry()) != null) {
		        	uncompressedEntryIn = entry.getInputStream();
		        	while ((read = uncompressedEntryIn.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	md5.update(buffer, 0, read);
		        	}
		        	uncompressedEntryIn.close();
		        	uncompressedEntryIn = null;
		        	entry.close();
		        	entry = null;
		        }
		        reader.close();
		        reader = null;

		        md5DigestBytesVerify = md5.digest();
		        md5compDigestBytesVerify = md5comp.digest();
		        bVerified = ArrayUtils.equalsAt(md5DigestBytesVerify, md5DigestBytesOrg, 0);

	        	// debug
		        //System.out.println("    original md5:     " + Base16.encodeArray(md5DigestBytesOrg));
		        //System.out.println("decompressed md5:     " + Base16.encodeArray(md5DigestBytesVerify));
		        //System.out.println("  compressed md5:     " + Base16.encodeArray(md5compDigestBytesVerify));
		        //System.out.println(bVerified);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			closeIOQuietly(pin);
			closeIOQuietly(arcRecord);
			closeIOQuietly(arcReader);
	        closeIOQuietly(in);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
			closeIOQuietly(uncompressedEntryIn);
			closeIOQuietly(entry);
			closeIOQuietly(reader);
		}
	}

	// TODO
	protected void compressWarcFile(InputStream in) {
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] endMark = "\r\n\r\n".getBytes();
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
		WarcReader warcReader = null;
		WarcRecord warcRecord = null;
        OutputStream cout = null;
		Payload payload;
		int read;
		InputStream pin = null;
        MessageDigest md5 = null;
        MessageDigest md5comp = null;
        InputStream uncompressedFileIn = null;
        GzipReader reader = null;
        InputStream uncompressedEntryIn = null;
        bVerified = false;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(compressionLevel);
	        entry = null;

	        if (bVerify) {
		        md5 = MessageDigest.getInstance("MD5");
		        md5comp = MessageDigest.getInstance("MD5");
	        }

	        warcReader = WarcReaderFactory.getReader( in );
			warcReader.setBlockDigestEnabled( true );
			warcReader.setPayloadDigestEnabled( true );
			while ( (warcRecord = warcReader.getNextRecord()) != null ) {
		        entry = new GzipEntry();
		        entry.magic = GzipConstants.GZIP_MAGIC;
		        entry.cm = GzipConstants.CM_DEFLATE;
		        entry.flg = 0;
		        entry.mtime = System.currentTimeMillis() / 1000;
		        entry.xfl = 0;
		        entry.os = GzipConstants.OS_UNKNOWN;
		        writer.writeEntryHeader(entry);

		        cout = entry.getOutputStream();
		        cout.write(warcRecord.header.headerBytes);
		        if (bVerify) {
		        	md5.update(warcRecord.header.headerBytes);
		        }

				payload = warcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	cout.write(buffer, 0, read);
			        	if (bVerify) {
				        	md5.update(buffer, 0, read);
			        	}
			        }
			        pin.close();
			        pin = null;
				}

				cout.write(endMark);
				if (bVerify) {
		        	md5.update(endMark);
				}
				cout.close();
				cout = null;

				warcRecord.close();
				warcRecord = null;
			}
			writer.close();
			writer = null;

			if (bVerify) {
		        md5DigestBytesOrg = md5.digest();

		        md5.reset();

		        uncompressedFileIn = new FileInputStream(dstFile);
		        uncompressedFileIn = new DigestInputStreamNoSkip(uncompressedFileIn, md5comp);
		        reader = new GzipReader(uncompressedFileIn, GZIP_OUTPUT_BUFFER_SIZE);
		        while ((entry = reader.getNextEntry()) != null) {
		        	uncompressedEntryIn = entry.getInputStream();
		        	while ((read = uncompressedEntryIn.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	md5.update(buffer, 0, read);
		        	}
		        	uncompressedEntryIn.close();
		        	uncompressedEntryIn = null;
		        	entry.close();
		        	entry = null;
		        }
		        reader.close();
		        reader = null;

		        md5DigestBytesVerify = md5.digest();
		        md5compDigestBytesVerify = md5comp.digest();
		        bVerified = ArrayUtils.equalsAt(md5DigestBytesVerify, md5DigestBytesOrg, 0);

	        	// debug
		        //System.out.println("    original md5:     " + Base16.encodeArray(md5DigestBytesOrg));
		        //System.out.println("decompressed md5:     " + Base16.encodeArray(md5DigestBytesVerify));
		        //System.out.println("  compressed md5:     " + Base16.encodeArray(md5compDigestBytesVerify));
		        //System.out.println(bVerified);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			closeIOQuietly(pin);
			closeIOQuietly(warcRecord);
			closeIOQuietly(warcReader);
	        closeIOQuietly(in);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
			closeIOQuietly(uncompressedEntryIn);
			closeIOQuietly(entry);
			closeIOQuietly(reader);
		}
	}

	public static void closeIOQuietly(Closeable closable) {
		if (closable != null) {
	        try {
	        	closable.close();
			} catch (IOException e) {
			}
		}
	}

}
