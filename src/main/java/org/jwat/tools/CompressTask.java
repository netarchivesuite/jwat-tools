package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.Deflater;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class CompressTask extends Task {

	private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	public CompressTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		// Compression level.
		argument = arguments.idMap.get( JWATTools.A_COMPRESS );
		if (argument != null) {
			int compressionLevel = argument.argDef.subId;
			System.out.println( "Compression level: " + compressionLevel );
		}
		// Files
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		String srcFname = srcFile.getName();
		ByteCountingPushBackInputStream pbin = null;
		RandomAccessFile raf = null;
		try {
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( srcFile ), 8192 ), 16 );
			if (!GzipReader.isGzipped(pbin)) {
				String dstFname = srcFname + ".gz";
				File dstFile = new File( srcFile.getParentFile(), dstFname );
				if ( !dstFile.exists() ) {
					System.out.println( srcFname + " -> " + dstFname );
					if ( ArcReaderFactory.isArcFile( pbin ) ) {
						compressArcFile( pbin );
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
					}
					/*
					GzipReader gzipReader = new GzipReader( pbin );
					GzipReaderEntry gzipEntry;
					InputStream in;
					raf = new RandomAccessFile( dstFile, "rw" );
					byte[] buffer = new byte[ 8192 ];
					int read;
					while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
						in = gzipEntry.getInputStream();
						while ( (read = in.read(buffer)) != -1 ) {
							raf.write( buffer, 0, read );
						}
						in.close();
						gzipEntry.close();
					}
					raf.close();
					gzipReader.close();
					*/
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
				}
				catch (IOException e) {
				}
			}
		}
	}

	protected void compressArcFile(InputStream in) throws IOException {
		ArcReader arcReader = ArcReaderFactory.getReaderUncompressed( in );
		arcReader.setBlockDigestEnabled( true );
		arcReader.setPayloadDigestEnabled( true );
		ArcVersionBlock version = arcReader.getVersionBlock();
		if ( version != null ) {
			boolean b = true;
			while ( b ) {
				ArcRecord arcRecord = arcReader.getNextRecord();
				if ( arcRecord != null ) {
				    arcRecord.close();
				}
				else {
					b = false;
				}
			}
		}
		arcReader.close();
	}

	protected void compressWarcFile(InputStream in) throws IOException {
		WarcReader warcReader = WarcReaderFactory.getReader( in );
		warcReader.setBlockDigestEnabled( true );
		warcReader.setPayloadDigestEnabled( true );
		WarcRecord warcRecord;
		while ( (warcRecord = warcReader.getNextRecord()) != null ) {
			warcRecord.close();
		}
		warcReader.close();
	}

}
