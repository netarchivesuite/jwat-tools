package org.jwat.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipInputStream;

public class DecompressTask extends Task {

	public DecompressTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		String srcFname = srcFile.getName();
		if ( srcFname.toLowerCase().endsWith( ".gz" ) ) {
			String dstFname = srcFname.substring( 0, srcFname.length() - 3 );
			File dstFile = new File( srcFile.getParentFile(), dstFname );
			if ( !dstFile.exists() ) {
				System.out.println( srcFname + " -> " + dstFname );
				try {
					GzipInputStream gzin = new GzipInputStream( new FileInputStream( srcFile ) );
					GzipEntry entry;
					InputStream in;
					RandomAccessFile raf = new RandomAccessFile( dstFile, "rw" );
					byte[] buffer = new byte[ 8192 ];
					int read;
					while ( (entry = gzin.getNextEntry()) != null ) {
						in = gzin.getEntryInputStream();
						while ( (read = in.read(buffer)) != -1 ) {
							raf.write( buffer, 0, read );
						}
						in.close();
					}
					raf.close();
					gzin.close();
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println( dstFile.getName() + " already exists, skipping." );
			}
		}
	}

}
