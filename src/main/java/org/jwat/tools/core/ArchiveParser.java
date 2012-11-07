package org.jwat.tools.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class ArchiveParser {

	/*
	 * Settings.
	 */

	public UriProfile uriProfile = UriProfile.RFC3986;

	public boolean bBlockDigestEnabled = true;

	public boolean bPayloadDigestEnabled = true;

    public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;


	/*
	 * State.
	 */

	protected RandomAccessFile raf = null;
	protected RandomAccessFileInputStream rafin;
	protected ByteCountingPushBackInputStream pbin = null;

	public GzipReader gzipReader = null;
	public ArcReader arcReader = null;
	public WarcReader warcReader = null;

	protected GzipEntry gzipEntry = null;
	protected ArcRecordBase arcRecord = null;
	protected WarcRecord warcRecord = null;

	protected byte[] buffer = new byte[ 8192 ];

	public ArchiveParser() {
	}

	public long parse(File file, ArchiveParserCallback callbacks) {
		try {
			raf = new RandomAccessFile( file, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, 8192 ), 16 );
			if ( GzipReader.isGzipped( pbin ) ) {
				gzipReader = new GzipReader( pbin );
				ByteCountingPushBackInputStream in;
				int gzipEntries = 0;
				while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
					in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzipEntry.getInputStream(), 8192 ), 16 );
					++gzipEntries;
					//System.out.println(gzipEntries + " - " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")");
					if ( gzipEntries == 1 ) {
						if ( ArcReaderFactory.isArcFile( in ) ) {
							arcReader = ArcReaderFactory.getReaderUncompressed();
							arcReader.setUriProfile(uriProfile);
							arcReader.setBlockDigestEnabled( bBlockDigestEnabled );
							arcReader.setPayloadDigestEnabled( bPayloadDigestEnabled );
						    arcReader.setRecordHeaderMaxSize( recordHeaderMaxSize );
						    arcReader.setPayloadHeaderMaxSize( payloadHeaderMaxSize );
							callbacks.apcFileId(file, FileIdent.FILEID_ARC_GZ);
						}
						else if ( WarcReaderFactory.isWarcFile( in ) ) {
							warcReader = WarcReaderFactory.getReaderUncompressed();
							warcReader.setWarcTargerUriProfile(uriProfile);
							warcReader.setBlockDigestEnabled( bBlockDigestEnabled );
							warcReader.setPayloadDigestEnabled( bPayloadDigestEnabled );
						    warcReader.setRecordHeaderMaxSize( recordHeaderMaxSize );
						    warcReader.setPayloadHeaderMaxSize( payloadHeaderMaxSize );
							callbacks.apcFileId(file, FileIdent.FILEID_WARC_GZ);
						}
						else {
							callbacks.apcFileId(file, FileIdent.FILEID_GZIP);
						}
					}
					if ( arcReader != null ) {
						while ( (arcRecord = arcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() )) != null ) {
							callbacks.apcArcRecordStart(arcRecord, gzipReader.getStartOffset(), true);
						}
					}
					else if ( warcReader != null ) {
						while ( (warcRecord = warcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() ) ) != null ) {
							callbacks.apcWarcRecordStart(warcRecord, gzipReader.getStartOffset(), true);
						}
					}
					else {
						while ( in.read(buffer) != -1 ) {
						}
					}
					in.close();
					gzipEntry.close();
					callbacks.apcGzipEntryStart(gzipEntry, gzipReader.getStartOffset());
					callbacks.apcUpdateConsumed(pbin.getConsumed());
				}
			}
			else if ( ArcReaderFactory.isArcFile( pbin ) ) {
				arcReader = ArcReaderFactory.getReaderUncompressed( pbin );
				arcReader.setUriProfile(uriProfile);
				arcReader.setBlockDigestEnabled( bBlockDigestEnabled );
				arcReader.setPayloadDigestEnabled( bPayloadDigestEnabled );
			    arcReader.setRecordHeaderMaxSize( recordHeaderMaxSize );
			    arcReader.setPayloadHeaderMaxSize( payloadHeaderMaxSize );
				callbacks.apcFileId(file, FileIdent.FILEID_ARC);
				while ( (arcRecord = arcReader.getNextRecord()) != null ) {
					callbacks.apcArcRecordStart(arcRecord, arcReader.getStartOffset(), false);
					callbacks.apcUpdateConsumed(pbin.getConsumed());
				}
				arcReader.close();
			}
			else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
				warcReader = WarcReaderFactory.getReaderUncompressed( pbin );
				warcReader.setWarcTargerUriProfile(uriProfile);
				warcReader.setBlockDigestEnabled( bBlockDigestEnabled );
				warcReader.setPayloadDigestEnabled( bPayloadDigestEnabled );
			    warcReader.setRecordHeaderMaxSize( recordHeaderMaxSize );
			    warcReader.setPayloadHeaderMaxSize( payloadHeaderMaxSize );
				callbacks.apcFileId(file, FileIdent.FILEID_WARC);
				while ( (warcRecord = warcReader.getNextRecord()) != null ) {
					callbacks.apcWarcRecordStart(warcRecord, warcReader.getStartOffset(), false);
					callbacks.apcUpdateConsumed(pbin.getConsumed());
				}
				warcReader.close();
			}
			else {
				callbacks.apcFileId(file, FileIdent.FILEID_UNKNOWN);
			}
			callbacks.apcDone();
		}
		catch (Throwable t) {
			// TODO just use reader.getStartOffset?
			long startOffset = -1;
			Long length = null;
			if (arcRecord != null) {
				startOffset = arcRecord.getStartOffset();
				length = arcRecord.header.archiveLength;
			}
			if (warcRecord != null) {
				startOffset = warcRecord.getStartOffset();
				length = warcRecord.header.contentLength;
			}
			if (gzipEntry != null) {
				startOffset = gzipEntry.getStartOffset();
				// TODO correct entry size including header+trailer.
				length = gzipEntry.compressed_size;
			}
			if (length != null) {
				startOffset += length;
			}
			callbacks.apcRuntimeError(t, startOffset, pbin.getConsumed());
		}
		finally {
			if ( arcReader != null ) {
				arcReader.close();
			}
			if ( warcReader != null ) {
				warcReader.close();
			}
			if (gzipReader != null) {
				try {
					gzipReader.close();
				}
				catch (IOException e) {
				}
			}
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
				}
				catch (IOException e) {
				}
			}
		}
		return pbin.getConsumed();
	}

}
