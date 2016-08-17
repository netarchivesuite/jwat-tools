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

	/**
	 * Compress input file according to its type.
	 * ARC/WARC files are compressed one record at a time and concatenated into on multi-entry GZip file.
	 * Other files are compressed as one entry.
	 * @param srcFile
	 */
	protected CompressionResult compressFile(File srcFile, CompressionOptions options) {
		CompressionResult result = null;
		String srcFname = srcFile.getName();
		String dstFname = srcFname + ".gz";
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin = null;
		ByteCountingPushBackInputStream pbin = null;
		File dstFile;
		try {
			raf = new RandomAccessFile( srcFile, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, INPUT_BUFFER_SIZE ), 32 );
			if (!GzipReader.isGzipped(pbin)) {
				if (options.dstPath == null) {
					dstFile = new File( srcFile.getParentFile(), dstFname );
				}
				else {
					dstFile = new File( options.dstPath, dstFname );
				}
				if ( !dstFile.exists() ) {
					//System.out.println( srcFname + " -> " + dstFname );
					if ( ArcReaderFactory.isArcFile( pbin ) ) {
						result = compressArcFile( pbin, dstFile, options );
						result.srcFile = srcFile;
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
						result = compressWarcFile( pbin, dstFile, options );
						result.srcFile = srcFile;
					}
					else {
						result = compressNormalFile( pbin, dstFile, options );
						result.srcFile = srcFile;
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
			closeIOQuietly(pbin);
			closeIOQuietly(rafin);
			closeIOQuietly(raf);
		}
		return result;
	}

	// TODO
	protected CompressionResult compressNormalFile(InputStream in, File dstFile, CompressionOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
        int read;
        MessageDigest md5 = null;
        MessageDigest md5comp = null;
        CompressionResult result = new CompressionResult();
        result.dstFile = dstFile;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE );
	        writer.setCompressionLevel(options.compressionLevel);

	        if (options.bVerify) {
		        md5 = MessageDigest.getInstance("MD5");
		        md5comp = MessageDigest.getInstance("MD5");
	        }

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
	        	if (options.bVerify) {
		        	md5.update(buffer, 0, read);
	        	}
	        }

	        cout.close();
	        cout = null;
	        entry.close();
	        entry = null;
	        writer.close();
	        writer = null;
	        in.close();
	        in = null;
	        result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
	        closeIOQuietly(in);
			closeIOQuietly(entry);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
		}
		return result;
	}

	// TODO
	protected CompressionResult compressArcFile(InputStream in, File dstFile, CompressionOptions options) {
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
        CompressionResult result = new CompressionResult();
        result.dstFile = dstFile;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(options.compressionLevel);

	        if (options.bVerify) {
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
		        if (options.bVerify) {
		        	md5.update(arcRecord.header.headerBytes);
		        }

				payload = arcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	cout.write(buffer, 0, read);
			        	if (options.bVerify) {
				        	md5.update(buffer, 0, read);
			        	}
			        }
				}

				cout.close();
				cout = null;
				entry.close();
				entry = null;
		        arcRecord.close();
		        arcRecord = null;
			}
			writer.close();
			writer = null;
			arcReader.close();
			arcReader = null;
			in.close();
			in = null;

			if (options.bVerify) {
				result.md5DigestBytesOrg = md5.digest();

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

		        result.md5DigestBytesVerify = md5.digest();
		        result.md5compDigestBytesVerify = md5comp.digest();
		        result.bVerified = ArrayUtils.equalsAt(result.md5DigestBytesVerify, result.md5DigestBytesOrg, 0);

	        	// debug
		        //System.out.println("    original md5:     " + Base16.encodeArray(md5DigestBytesOrg));
		        //System.out.println("decompressed md5:     " + Base16.encodeArray(md5DigestBytesVerify));
		        //System.out.println("  compressed md5:     " + Base16.encodeArray(md5compDigestBytesVerify));
		        //System.out.println(bVerified);
			}
	        result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
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
		return result;
	}

    protected byte[] endMark = "\r\n\r\n".getBytes();

    // TODO
	protected CompressionResult compressWarcFile(InputStream in, File dstFile, CompressionOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
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
        CompressionResult result = new CompressionResult();
        result.dstFile = dstFile;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(options.compressionLevel);

	        if (options.bVerify) {
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
		        if (options.bVerify) {
		        	md5.update(warcRecord.header.headerBytes);
		        }

		        // debug
		        //System.out.println(warcRecord.getStartOffset());
		        //System.out.println(warcRecord.getConsumed());

		        payload = warcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	cout.write(buffer, 0, read);
			        	if (options.bVerify) {
				        	md5.update(buffer, 0, read);
			        	}
			        }
			        pin.close();
			        pin = null;
				}

				cout.write(endMark);
				if (options.bVerify) {
		        	md5.update(endMark);
				}

				cout.close();
				cout = null;
				entry.close();
				entry = null;
				warcRecord.close();
				warcRecord = null;
			}
			writer.close();
			writer = null;
			warcReader.close();
			warcReader = null;
			in.close();
			in = null;

			if (options.bVerify) {
				result.md5DigestBytesOrg = md5.digest();

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

		        result.md5DigestBytesVerify = md5.digest();
		        result.md5compDigestBytesVerify = md5comp.digest();
		        result.bVerified = ArrayUtils.equalsAt(result.md5DigestBytesVerify, result.md5DigestBytesOrg, 0);

	        	// debug
		        //System.out.println("    original md5:     " + Base16.encodeArray(md5DigestBytesOrg));
		        //System.out.println("decompressed md5:     " + Base16.encodeArray(md5DigestBytesVerify));
		        //System.out.println("  compressed md5:     " + Base16.encodeArray(md5compDigestBytesVerify));
		        //System.out.println(bVerified);
			}
	        result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
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
		return result;
	}

	public static void closeIOQuietly(Closeable closable) {
		if (closable != null) {
	        try {
	        	closable.close();
			}
	        catch (IOException e) {
			}
		}
	}

}
