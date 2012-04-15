package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.Deflater;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.Payload;
import org.jwat.gzip.GzipConstants;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.gzip.GzipWriter;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class CompressTask extends Task {

	protected int compressionLevel = Deflater.DEFAULT_COMPRESSION;

	public CompressTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		// Compression level.
		argument = arguments.idMap.get( JWATTools.A_COMPRESS );
		if (argument != null) {
			compressionLevel = argument.argDef.subId;
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
						compressArcFile( pbin, dstFile );
					}
					else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
						compressWarcFile( pbin, dstFile );
					} else {
						compressNormalFile( pbin, dstFile );
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
				}
				catch (IOException e) {
				}
			}
		}
	}

	protected void compressNormalFile(InputStream in, File dstFile) {
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
        byte[] buffer = new byte[16384];
        int read;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out);

	        entry = new GzipEntry();
	        entry.magic = GzipConstants.GZIP_MAGIC;
	        entry.cm = GzipConstants.CM_DEFLATE;
	        entry.flg = 0;
	        entry.mtime = System.currentTimeMillis() / 1000;
	        entry.xfl = 0;
	        entry.os = GzipConstants.OS_UNKNOWN;
	        writer.writeEntryHeader(entry);

	        cout = entry.getOutputStream();

	        while ((read = in.read(buffer, 0, 16384)) != -1) {
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

	protected void compressArcFile(InputStream in, File dstFile) {
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
        OutputStream cout = null;
		ArcReader arcReader = null;
		ArcVersionBlock version;
		ArcRecord arcRecord;
		Payload payload;
		int read;
        byte[] buffer = new byte[16384];
		InputStream pin;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out);
	        entry = null;

	        arcReader = ArcReaderFactory.getReaderUncompressed( in );
			arcReader.setBlockDigestEnabled( true );
			arcReader.setPayloadDigestEnabled( true );
			version = arcReader.getVersionBlock();
			if ( version != null ) {
		        entry = new GzipEntry();
		        entry.magic = GzipConstants.GZIP_MAGIC;
		        entry.cm = GzipConstants.CM_DEFLATE;
		        entry.flg = 0;
		        entry.mtime = System.currentTimeMillis() / 1000;
		        entry.xfl = 0;
		        entry.os = GzipConstants.OS_UNKNOWN;
		        writer.writeEntryHeader(entry);

		        cout = entry.getOutputStream();
		        //cout.write(version.headerBytes);

				payload = version.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, 16384)) != -1) {
			        	cout.write(buffer, 0, read);
			        }
				}

				cout.close();

				version.close();

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
			        //cout.write(arcRecord.headerBytes);

					payload = arcRecord.getPayload();
					if (payload != null) {
						pin = payload.getInputStreamComplete();
				        while ((read = pin.read(buffer, 0, 16384)) != -1) {
				        	cout.write(buffer, 0, read);
				        }
					}

					cout.close();

			        arcRecord.close();
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			arcReader.close();
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
				} catch (IOException e1) {
				}
			}
		}
	}

	protected void compressWarcFile(InputStream in, File dstFile) {
		FileOutputStream out = null;
        GzipWriter writer = null;
        GzipEntry entry = null;
		WarcReader warcReader = null;
		WarcRecord warcRecord;
        OutputStream cout = null;
		Payload payload;
		int read;
        byte[] buffer = new byte[16384];
        byte[] endMark = "\r\n\r\n".getBytes();
		InputStream pin;
		try {
			out = new FileOutputStream(dstFile, false);
	        writer = new GzipWriter(out);
	        entry = null;

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

				payload = warcRecord.getPayload();
				if (payload != null) {
					pin = payload.getInputStreamComplete();
			        while ((read = pin.read(buffer, 0, 16384)) != -1) {
			        	cout.write(buffer, 0, read);
			        }
				}

				cout.write(endMark);
				cout.close();

				warcRecord.close();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			warcReader.close();
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
				} catch (IOException e1) {
				}
			}
		}
	}

}
