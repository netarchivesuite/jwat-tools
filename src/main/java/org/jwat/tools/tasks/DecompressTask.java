package org.jwat.tools.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.gzip.GzipReader;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.CommandLine;
import org.jwat.tools.JWATTools;
import org.jwat.tools.Task;
import org.jwat.tools.CommandLine.Argument;
import org.jwat.tools.CommandLine.Arguments;

public class DecompressTask extends Task {

	public DecompressTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
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
			if (GzipReader.isGzipped(pbin)) {
				String dstFname;
				if (srcFname.endsWith(".gz")) {
					dstFname = srcFname.substring( 0, srcFname.length() - 3 );
				}
				else {
					dstFname = srcFname + ".org";
				}
				File dstFile = new File( srcFile.getParentFile(), dstFname );
				if ( !dstFile.exists() ) {
					System.out.println( srcFname + " -> " + dstFname );
					GzipReader gzipReader = new GzipReader( pbin );
					GzipEntry gzipEntry;
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
					gzipReader.close();
				}
				else {
					System.out.println( dstFile.getName() + " already exists, skipping." );
				}
			}
			else if ( srcFname.toLowerCase().endsWith( ".gz" ) ) {
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

}
