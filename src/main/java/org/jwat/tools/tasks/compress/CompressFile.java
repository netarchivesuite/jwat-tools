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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jwat.arc.ArcHeader;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ArrayUtils;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.DigestInputStreamNoSkip;
import org.jwat.common.HeaderLine;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.PayloadWithHeaderAbstract;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.RandomAccessFileOutputStream;
import org.jwat.gzip.GzipConstants;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.gzip.GzipWriter;
import org.jwat.tools.tasks.compress.JSONSerializer.JSONSerializerFactory;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import com.antiaction.common.json.annotation.JSONNullable;
import com.antiaction.common.json.annotation.JSONTypeInstance;

public class CompressFile {

	//private static final int INPUT_BUFFER_SIZE = 1024 * 1024;
	private static final int INPUT_BUFFER_SIZE = 16384;

	//private static final int GZIP_OUTPUT_BUFFER_SIZE = 1024 * 1024;
	private static final int GZIP_OUTPUT_BUFFER_SIZE = 16384;

	//private static final int BUFFER_SIZE = 65 * 1024;
	private static final int BUFFER_SIZE = 8192;

	private static ThreadLocalObjectPool<JSONSerializer> jsonTLPool;

	static {
		jsonTLPool = ThreadLocalObjectPool.getPool(new JSONSerializerFactory());
	}

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
						result = compressArcFile( raf, pbin, srcFile, dstFile, options );
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
						result = compressWarcFile( raf, pbin, srcFile, dstFile, options );
					}
					else {
						result = compressNormalFile( pbin, srcFile, dstFile, options );
					}
				}
				else {
					System.out.println( dstFile.getName() + " already exists, skipping." );
			        result = new CompressResult();
			        result.srcFile = srcFile;
				}
			}
			else if ( !srcFname.toLowerCase().endsWith( ".gz" ) ) {
				System.out.println( "Invalid extension: " + srcFname );
		        result = new CompressResult();
		        result.srcFile = srcFile;
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
		if (result == null) {
	        result = new CompressResult();
	        result.srcFile = srcFile;
		}
		return result;
	}

	// TODO
	protected CompressResult compressNormalFile(InputStream in, File srcFile, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
        int read;
        MessageDigest md5 = null;
        MessageDigest md5comp = null;
        CompressResult result = new CompressResult();
		result.srcFile = srcFile;
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

    protected byte[] arcEndMark = "\n".getBytes();

	// TODO
	protected CompressResult compressArcFile(RandomAccessFile raf, InputStream in, File srcFile, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream uncompressedFileIn = null;
        RandomAccessFile rafOut = null;
		OutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
		ArcReader arcReader = null;
		ArcRecordBase arcRecord = null;
        String scheme = null;
    	Long count;
        int idx;
		Payload payload;
		int read;
		InputStream pin = null;
        MessageDigest md5uncomp = null;
        MessageDigest md5comp = null;
        InputStream compressedFileIn = null;
        GzipReader reader = null;
        InputStream uncompressedEntryIn = null;
        CompressResult result = new CompressResult();
		result.srcFile = srcFile;
        result.dstFile = dstFile;
        JSONSerializer jser = null;
        RecordEntry recordEntry = null;
        try {
			if (options.bHeaderFiles) {
				jser = jsonTLPool.getThreadLocalObject();
				jser.open(result, options);
			}

            rafOut = new RandomAccessFile(dstFile, "rw");
            out = new RandomAccessFileOutputStream(rafOut);
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

	        result.schemesMap = new HashMap<String, Long>();

	        arcReader = ArcReaderFactory.getReaderUncompressed( uncompressedFileIn );
			arcReader.setBlockDigestEnabled( false );
			arcReader.setPayloadDigestEnabled( false );
			while ((arcRecord = arcReader.getNextRecord()) != null) {
				if (options.bHeaderFiles) {
			        recordEntry = new RecordEntry();
		        	recordEntry.aL = arcHeaderToNameValueList(arcRecord.header);
		        	recordEntry.i = rafOut.getFilePointer();
				}
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

		        scheme = null;
		        if (arcRecord.header.urlUri != null) {
		        	scheme = arcRecord.header.urlUri.getScheme();
		        }
		        if (scheme == null) {
		        	if (arcRecord.header.urlStr != null) {
		        		idx = arcRecord.header.urlStr.indexOf("://");
		        		if (idx > 0) {
		        			scheme = arcRecord.header.urlStr.substring(0, idx);
		        		}
		        	}
		        }
		        if (scheme != null) {
		        	count = result.schemesMap.get(scheme);
		        	if (count == null) {
		        		count = 0L;
		        	}
		        	result.schemesMap.put(scheme, count + 1);
		        }

		        cout = entry.getOutputStream();

		        if (!options.bTwopass) {
		        	/*
		        	 * Write the "raw" data read by the ARC reader.
		        	 */
			        cout.write(arcRecord.header.headerBytes);
			        /*
			        if (options.bVerify) {
			        	md5uncomp.update(arcRecord.header.headerBytes);
			        }
			        */
					payload = arcRecord.getPayload();
					if (payload != null) {
						if (options.bHeaderFiles) {
							PayloadWithHeaderAbstract wrappedHeader = payload.getPayloadHeaderWrapped();
							if (wrappedHeader instanceof HttpHeader) {
					        	recordEntry.hL = headerLinesToNameValueList(((HttpHeader)wrappedHeader).getHeaderList());
							}
						}
						// Payload
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
					cout.write(arcEndMark);
					/*
					if (options.bVerify) {
			        	md5uncomp.update(arcEndMark);
					}
					*/
			        arcRecord.close();
			        arcRecord = null;
		        }
		        else {
		        	/*
		        	 * Use ARC reader to get offset and length of record and read directly from file.
		        	 */
					payload = arcRecord.getPayload();
					if (payload != null) {
						if (options.bHeaderFiles) {
							PayloadWithHeaderAbstract wrappedHeader = payload.getPayloadHeaderWrapped();
							if (wrappedHeader instanceof HttpHeader) {
					        	recordEntry.hL = headerLinesToNameValueList(((HttpHeader)wrappedHeader).getHeaderList());
							}
						}
						// Payload
						pin = payload.getInputStreamComplete();
				        pin.close();
				        pin = null;
				        payload.close();
					}
					arcRecord.close();
			        long offset  = arcRecord.getStartOffset();
			        long consumed = arcRecord.getConsumed();
					arcRecord = null;
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
				if (options.bHeaderFiles) {
		        	recordEntry.l = rafOut.getFilePointer() - recordEntry.i;
					jser.serialize(recordEntry);
				}
			}
			writer.close();
			writer = null;
			out.close();
			out = null;
			rafOut.close();
			rafOut = null;
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

			if (options.bHeaderFiles) {
				jser.close();
				jser = null;
			}

			result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
			closeIOQuietly(jser);
			closeIOQuietly(pin);
			closeIOQuietly(arcRecord);
			closeIOQuietly(arcReader);
	        closeIOQuietly(uncompressedFileIn);
	        closeIOQuietly(in);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
			closeIOQuietly(rafOut);
			closeIOQuietly(uncompressedEntryIn);
			closeIOQuietly(entry);
			closeIOQuietly(reader);
		}
		return result;
	}

    protected byte[] warcEndMark = "\r\n\r\n".getBytes();

    // TODO
	protected CompressResult compressWarcFile(RandomAccessFile raf, InputStream in, File srcFile, File dstFile, CompressOptions options) {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream uncompressedFileIn = null;
        RandomAccessFile rafOut = null;
		OutputStream out = null;
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
		result.srcFile = srcFile;
        result.dstFile = dstFile;
        JSONSerializer jser = null;
        RecordEntry recordEntry = null;
        try {
			if (options.bHeaderFiles) {
				jser = jsonTLPool.getThreadLocalObject();
				jser.open(result, options);
			}

            rafOut = new RandomAccessFile(dstFile, "rw");
            out = new RandomAccessFileOutputStream(rafOut);
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
				if (options.bHeaderFiles) {
			        recordEntry = new RecordEntry();
		        	recordEntry.wL = headerLinesToNameValueList(warcRecord.header.getHeaderList());
		        	recordEntry.i = rafOut.getFilePointer();
				}
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
			        payload = warcRecord.getPayload();
					if (payload != null) {
						if (options.bHeaderFiles) {
							PayloadWithHeaderAbstract wrappedHeader = payload.getPayloadHeaderWrapped();
							if (wrappedHeader instanceof HttpHeader) {
					        	recordEntry.hL = headerLinesToNameValueList(((HttpHeader)wrappedHeader).getHeaderList());
							}
						}
						// Payload
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
					cout.write(warcEndMark);
					/*
					if (options.bVerify) {
			        	md5uncomp.update(warcEndMark);
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
						if (options.bHeaderFiles) {
							PayloadWithHeaderAbstract wrappedHeader = payload.getPayloadHeaderWrapped();
							if (wrappedHeader instanceof HttpHeader) {
					        	recordEntry.hL = headerLinesToNameValueList(((HttpHeader)wrappedHeader).getHeaderList());
							}
						}
						// Payload
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
				if (options.bHeaderFiles) {
		        	recordEntry.l = rafOut.getFilePointer() - recordEntry.i;
					jser.serialize(recordEntry);
				}
			}
			writer.close();
			writer = null;
			out.close();
			out = null;
			rafOut.close();
			rafOut = null;
			warcReader.close();
			warcReader = null;
			uncompressedFileIn.close();
			uncompressedFileIn = null;
			in.close();
			in = null;

			if (options.bVerify) {
				result.md5DigestBytesOrg = md5uncomp.digest();

		        md5uncomp.reset();

	            //entryIdx = 0;

		        compressedFileIn = new FileInputStream(dstFile);
		        compressedFileIn = new DigestInputStreamNoSkip(compressedFileIn, md5comp);
		        reader = new GzipReader(compressedFileIn, GZIP_OUTPUT_BUFFER_SIZE);
		        while ((entry = reader.getNextEntry()) != null) {
		        	/*
		        	recordEntry = recordEntries.get(entryIdx++);
		        	if (recordEntry.i != entry.startOffset) {
		        		bInvalidOffset = true;
		        	}
		        	*/
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

			if (options.bHeaderFiles) {
				jser.close();
				jser = null;
			}

			result.bCompleted = true;
		}
		catch (Throwable t) {
			result.bCompleted = false;
			result.t = t;
			t.printStackTrace();
		}
		finally {
			closeIOQuietly(jser);
			closeIOQuietly(pin);
			closeIOQuietly(warcRecord);
			closeIOQuietly(warcReader);
			closeIOQuietly(uncompressedFileIn);
	        closeIOQuietly(in);
			closeIOQuietly(cout);
			closeIOQuietly(writer);
			closeIOQuietly(out);
			closeIOQuietly(rafOut);
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

	public List<NameValue> arcHeaderToNameValueList(ArcHeader header) {
		List<NameValue> nameValueList = new ArrayList<NameValue>();
	    if (header.urlStr != null ) {
    		nameValueList.add(new NameValue("url", header.urlStr));
	    }
	    if (header.ipAddressStr != null ) {
    		nameValueList.add(new NameValue("ip", header.ipAddressStr));
	    }
	    if (header.archiveDateStr != null ) {
    		nameValueList.add(new NameValue("archive-date", header.archiveDateStr));
	    }
	    if (header.contentTypeStr != null ) {
    		nameValueList.add(new NameValue("content-type", header.contentTypeStr));
	    }
	    if (header.resultCodeStr != null ) {
    		nameValueList.add(new NameValue("result-code", header.resultCodeStr));
	    }
	    if (header.checksumStr != null ) {
    		nameValueList.add(new NameValue("checksum", header.checksumStr));
	    }
	    if (header.locationStr != null ) {
    		nameValueList.add(new NameValue("location", header.locationStr));
	    }
	    if (header.offsetStr != null ) {
    		nameValueList.add(new NameValue("offset", header.offsetStr));
	    }
	    if (header.filenameStr != null ) {
    		nameValueList.add(new NameValue("filename", header.filenameStr));
	    }
	    if (header.archiveLengthStr != null ) {
    		nameValueList.add(new NameValue("archive-length", header.archiveLengthStr));
	    }
		return nameValueList;
	}

	public List<NameValue> headerLinesToNameValueList(List<HeaderLine> headerLines) {
		List<NameValue> nameValueList = null;
        Iterator<HeaderLine> hlIter;
        Iterator<HeaderLine> ahlIter;
        HeaderLine headerLine;
        if (headerLines != null) {
        	nameValueList = new ArrayList<NameValue>();
        	hlIter = headerLines.iterator();
        	while (hlIter.hasNext()) {
        		headerLine = hlIter.next();
        		nameValueList.add(new NameValue(headerLine.name, headerLine.value));
        		if (headerLine.lines != null && headerLine.lines.size() > 0) {
        			ahlIter = headerLines.iterator();
		        	while (ahlIter.hasNext()) {
		        		headerLine = ahlIter.next();
		        		nameValueList.add(new NameValue(headerLine.name, headerLine.value));
		        	}
        		}
        	}
        }
		return nameValueList;
	}

	public static class RecordEntry {
		public long i;
		public long l;
		@JSONTypeInstance(ArrayList.class)
		@JSONNullable
		public List<NameValue> aL;
		@JSONTypeInstance(ArrayList.class)
		@JSONNullable
		public List<NameValue> wL;
		@JSONTypeInstance(ArrayList.class)
		@JSONNullable
		public List<NameValue> hL;
		public RecordEntry() {
		}
	}

	public static class NameValue {
		public String n;
		public String v;
		public NameValue() {
		}
		public NameValue(String n, String v) {
			this.n = n;
			this.v = v;
		}
	}

}
