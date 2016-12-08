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
import java.util.Date;

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
	protected CompressResult compressFile(File srcFile, CompressOptions options) {
		CompressResult result = null;
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
						result = compressArcFile( raf, pbin, dstFile, options );
						result.srcFile = srcFile;
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
						result = compressWarcFile( raf, pbin, dstFile, options );
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
	protected CompressResult compressNormalFile(InputStream in, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
        int read;
        MessageDigest md5 = null;
        MessageDigest md5comp = null;
        CompressResult result = new CompressResult();
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
	protected CompressResult compressArcFile(RandomAccessFile raf, InputStream in, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream uncompressedFileIn = null;
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
		ArcReader arcReader = null;
		ArcRecordBase arcRecord = null;
		Payload payload;
		int read;
		InputStream pin = null;
        MessageDigest md5uncomp = null;
        MessageDigest md5comp = null;
        InputStream compressedFileIn = null;
        GzipReader reader = null;
        InputStream uncompressedEntryIn = null;
        CompressResult result = new CompressResult();
        result.dstFile = dstFile;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(options.compressionLevel);

	        if (options.bVerify) {
		        md5uncomp = MessageDigest.getInstance("MD5");
		        md5comp = MessageDigest.getInstance("MD5");
	        	uncompressedFileIn = new DigestInputStreamNoSkip(in, md5uncomp);
	        }
	        else {
	        	uncompressedFileIn = in;
	        }

	        arcReader = ArcReaderFactory.getReaderUncompressed( uncompressedFileIn );
			arcReader.setBlockDigestEnabled( false );
			arcReader.setPayloadDigestEnabled( false );
			while ((arcRecord = arcReader.getNextRecord()) != null) {
				Date date = arcRecord.header.archiveDate;
				if (date == null) {
					date = new Date(0L);
				}
		        entry = new GzipEntry();
		        entry.magic = GzipConstants.GZIP_MAGIC;
		        entry.cm = GzipConstants.CM_DEFLATE;
		        entry.flg = 0;
		        entry.mtime = date.getTime() / 1000;
		        entry.xfl = 0;
		        entry.os = GzipConstants.OS_UNKNOWN;
		        writer.writeEntryHeader(entry);

		        cout = entry.getOutputStream();
		        cout.write(arcRecord.header.headerBytes);
		        /*
		        if (options.bVerify) {
		        	md5uncomp.update(arcRecord.header.headerBytes);
		        }
		        */

				payload = arcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	cout.write(buffer, 0, read);
			        	/*
			        	if (options.bVerify) {
				        	md5uncomp.update(buffer, 0, read);
			        	}
			        	*/
			        }
			        pin.close();
			        pin = null;
			        payload.close();
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
			uncompressedFileIn.close();
			uncompressedFileIn = null;
			in.close();
			in = null;

			if (options.bVerify) {
				result.md5DigestBytesOrg = md5uncomp.digest();

		        md5uncomp.reset();

		        compressedFileIn = new FileInputStream(dstFile);
		        compressedFileIn = new DigestInputStreamNoSkip(compressedFileIn, md5comp);
		        reader = new GzipReader(compressedFileIn, GZIP_OUTPUT_BUFFER_SIZE);
		        while ((entry = reader.getNextEntry()) != null) {
		        	uncompressedEntryIn = entry.getInputStream();
		        	while ((read = uncompressedEntryIn.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	md5uncomp.update(buffer, 0, read);
		        	}
		        	uncompressedEntryIn.close();
		        	uncompressedEntryIn = null;
		        	entry.close();
		        	entry = null;
		        }
		        reader.close();
		        reader = null;

		        result.md5DigestBytesVerify = md5uncomp.digest();
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
	        closeIOQuietly(uncompressedFileIn);
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
	protected CompressResult compressWarcFile(RandomAccessFile raf, InputStream in, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream uncompressedFileIn = null;
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
		WarcReader warcReader = null;
		WarcRecord warcRecord = null;
        OutputStream cout = null;
		Payload payload;
		int read;
		InputStream pin = null;
        MessageDigest md5uncomp = null;
        MessageDigest md5comp = null;
        InputStream compressedFileIn = null;
        GzipReader reader = null;
        InputStream uncompressedEntryIn = null;
        CompressResult result = new CompressResult();
        result.dstFile = dstFile;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
	        writer.setCompressionLevel(options.compressionLevel);

	        if (options.bVerify) {
		        md5uncomp = MessageDigest.getInstance("MD5");
		        md5comp = MessageDigest.getInstance("MD5");
	        	uncompressedFileIn = new DigestInputStreamNoSkip(in, md5uncomp);
	        }
	        else {
	        	uncompressedFileIn = in;
	        }

	        warcReader = WarcReaderFactory.getReader( uncompressedFileIn );
			warcReader.setBlockDigestEnabled( true );
			warcReader.setPayloadDigestEnabled( true );
			while ( (warcRecord = warcReader.getNextRecord()) != null ) {
				Date date = warcRecord.header.warcDate;
				if (date == null) {
					date = new Date(0L);
				}
		        entry = new GzipEntry();
		        entry.magic = GzipConstants.GZIP_MAGIC;
		        entry.cm = GzipConstants.CM_DEFLATE;
		        entry.flg = 0;
		        entry.mtime = date.getTime() / 1000;
		        entry.xfl = 0;
		        entry.os = GzipConstants.OS_UNKNOWN;
		        writer.writeEntryHeader(entry);

		        cout = entry.getOutputStream();

		        if (!options.bTwopass) {
		        	/*
		        	 * Write the "raw" data read by the WARC reader.
		        	 */
			        cout.write(warcRecord.header.headerBytes);
			        /*
			        if (options.bVerify) {
			        	md5uncomp.update(warcRecord.header.headerBytes);
			        }
			        */

			        // debug
			        //System.out.println(warcRecord.getStartOffset());
			        //System.out.println(warcRecord.getConsumed());

			        payload = warcRecord.getPayload();
					if (payload != null) {
						pin = payload.getInputStreamComplete();
				        while ((read = pin.read(buffer, 0, BUFFER_SIZE)) != -1) {
				        	cout.write(buffer, 0, read);
				        	/*
				        	if (options.bVerify) {
					        	md5uncomp.update(buffer, 0, read);
				        	}
				        	*/
				        }
				        pin.close();
				        pin = null;
				        payload.close();
					}

					cout.write(endMark);
					/*
					if (options.bVerify) {
			        	md5uncomp.update(endMark);
					}
					*/
					warcRecord.close();
					warcRecord = null;
		        }
		        else {
		        	/*
		        	 * Use WARC reader to get offset and length of record and read directly from file.
		        	 */
			        payload = warcRecord.getPayload();
					if (payload != null) {
						pin = payload.getInputStreamComplete();
				        pin.close();
				        pin = null;
				        payload.close();
					}
					warcRecord.close();
			        long offset  = warcRecord.getStartOffset();
			        long consumed = warcRecord.getConsumed();
					warcRecord = null;
		        	long oldPos = raf.getFilePointer();
		        	raf.seek(offset);
		        	while (consumed > 0) {
		        		read = (int)Math.min(consumed, (long)buffer.length);
		        		read = raf.read(buffer, 0, read);
		        		if (read > 0) {
		        			consumed -= read;
				        	cout.write(buffer, 0, read);
		        		}
		        	}
		        	raf.seek(oldPos);
		        }

				cout.close();
				cout = null;
				entry.close();
				entry = null;
			}
			writer.close();
			writer = null;
			warcReader.close();
			warcReader = null;
			uncompressedFileIn.close();
			uncompressedFileIn = null;
			in.close();
			in = null;

			if (options.bVerify) {
				result.md5DigestBytesOrg = md5uncomp.digest();

		        md5uncomp.reset();

		        compressedFileIn = new FileInputStream(dstFile);
		        compressedFileIn = new DigestInputStreamNoSkip(compressedFileIn, md5comp);
		        reader = new GzipReader(compressedFileIn, GZIP_OUTPUT_BUFFER_SIZE);
		        while ((entry = reader.getNextEntry()) != null) {
		        	uncompressedEntryIn = entry.getInputStream();
		        	while ((read = uncompressedEntryIn.read(buffer, 0, BUFFER_SIZE)) != -1) {
			        	md5uncomp.update(buffer, 0, read);
		        	}
		        	uncompressedEntryIn.close();
		        	uncompressedEntryIn = null;
		        	entry.close();
		        	entry = null;
		        }
		        reader.close();
		        reader = null;
		        compressedFileIn.close();
		        compressedFileIn = null;

		        result.md5DigestBytesVerify = md5uncomp.digest();
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
			closeIOQuietly(uncompressedFileIn);
	        closeIOQuietly(in);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
			closeIOQuietly(uncompressedEntryIn);
			closeIOQuietly(entry);
			closeIOQuietly(reader);
			closeIOQuietly(compressedFileIn);
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
