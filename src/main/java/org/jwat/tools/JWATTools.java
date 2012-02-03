package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipInputStream;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class JWATTools {

	public static final int A_DECOMPRESS = 1;
	public static final int A_COMPRESS = 2;
	public static final int A_FILES = 3;
	public static final int A_TEST = 4;

	public static void main(String[] args) {
		JWATTools tools = new JWATTools();
		tools.Main( args );
	}

	public void Main(String[] args) {
		CommandLine.Arguments arguments = null;
		CommandLine cmdLine = new CommandLine();
		cmdLine.addOption( "-d", A_DECOMPRESS );
		cmdLine.addOption( "--decompress", A_DECOMPRESS );
		cmdLine.addOption( "-1", A_COMPRESS, 1 );
		cmdLine.addOption( "-2", A_COMPRESS, 2 );
		cmdLine.addOption( "-3", A_COMPRESS, 3 );
		cmdLine.addOption( "-4", A_COMPRESS, 4 );
		cmdLine.addOption( "-5", A_COMPRESS, 5 );
		cmdLine.addOption( "-6", A_COMPRESS, 6 );
		cmdLine.addOption( "-7", A_COMPRESS, 7 );
		cmdLine.addOption( "-8", A_COMPRESS, 8 );
		cmdLine.addOption( "-9", A_COMPRESS, 9 );
		cmdLine.addOption( "--fast", A_COMPRESS, 1 );
		cmdLine.addOption( "--best", A_COMPRESS, 9 );
		cmdLine.addOption( "-t", A_TEST );
		cmdLine.addOption( "--test", A_TEST );
		cmdLine.addListArgument( "files", A_FILES, 1, Integer.MAX_VALUE );
		try {
			arguments = cmdLine.parse( args );
			/*
			for ( int i=0; i<arguments.switchArgsList.size(); ++i) {
				argument = arguments.switchArgsList.get( i );
				System.out.println( argument.argDef.id + "," + argument.argDef.subId + "=" + argument.value );
			}
			*/
		}
		catch (CommandLine.ParseException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}
		if ( arguments == null ) {
			System.out.println( "JWATTools v0.1" );
			/*
			System.out.println( "usage: JWATTools [-dt19] [file ...]" );
			System.out.println( " -t --test        test compressed file integrity" );
			System.out.println( " -d --decompress  decompress" );
			System.out.println( " -1 --fast        compress faster" );
			System.out.println( " -9 --best        compress better" );
			*/
			System.out.println( "usage: JWATTools [-dt] [file ...]" );
			System.out.println( " -t   test compressed file integrity" );
			System.out.println( " -d   decompress" );
			//System.out.println( " -1   compress faster" );
			//System.out.println( " -9   compress better" );
		}
		else {
			if ( arguments.idMap.containsKey( A_DECOMPRESS ) ) {
				new DecompressTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_COMPRESS ) ) {
				new CompressTask( arguments );
			}
			else if ( arguments.idMap.containsKey( A_TEST ) ) {
				new TestTask( arguments );
			}
		}
	}

	static abstract class Task {
		public abstract void process(File file);
	}

	static void taskFileListFeeder(List<String> filesList, Task task) {
		String fileSeparator = System.getProperty( "file.separator" );
		File parentFile;
		String filepart;
		FilenameFilter filter;
		for ( int i=0; i<filesList.size(); ++i ) {
			filepart = filesList.get( i );
			int idx = filepart.lastIndexOf( fileSeparator );
			if ( idx != -1 ) {
				idx += fileSeparator.length();
				parentFile = new File( filepart.substring( 0, idx ) );
				filepart = filepart.substring( idx );
			}
			else {
				parentFile = new File( System.getProperty( "user.dir" ) );
			}
			idx = filepart.indexOf( "*" );
			if ( idx == -1 ) {
				parentFile = new File( parentFile, filepart );
				filepart = "";
				filter = new AcceptAllFilter();
			}
			else {
				filter = new AcceptAllFilter();
			}
			if ( parentFile.exists() ) {
				taskFileFeeder( parentFile, filter, task );
			}
			else {
				System.out.println( "File does not exist -- " + parentFile.getPath() );
				System.exit( 1 );
			}
		}
	}

	static void taskFileFeeder(File parentFile, FilenameFilter filter, Task task) {
		if ( parentFile.isFile() ) {
			task.process( parentFile );
		}
		else if ( parentFile.isDirectory() ) {
			File[] files = parentFile.listFiles();
			for ( int i=0; i<files.length; ++i ) {
				if ( files[ i ].isFile() ) {
					task.process( files[ i ] );
				}
				else {
					taskFileFeeder( files[ i ], filter, task );
				}
			}
		}
	}

	static class AcceptAllFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return true;
		}
	}

	static class DecompressTask extends Task {
		public DecompressTask(CommandLine.Arguments arguments) {
			CommandLine.Argument argument = arguments.idMap.get( A_FILES );
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
						RandomAccessFile ram = new RandomAccessFile( dstFile, "rw" );
						byte[] buffer = new byte[ 8192 ];
						int read;
						while ( (entry = gzin.getNextEntry()) != null ) {
							in = gzin.getEntryInputStream();
							while ( (read = in.read(buffer)) != -1 ) {
								ram.write( buffer, 0, read );
							}
							in.close();
						}
						ram.close();
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

	static class CompressTask extends Task {
		public CompressTask(CommandLine.Arguments arguments) {
			CommandLine.Argument argument = arguments.idMap.get( A_COMPRESS );
			int compressionLevel = argument.argDef.subId;
			System.out.println( "Compression level: " + compressionLevel );
			System.out.println( "Unsupported..." );
		}
		@Override
		public void process(File file) {
		}
	}

	static class TestTask extends Task {
		private int skipped = 0;
		public TestTask(CommandLine.Arguments arguments) {
			CommandLine.Argument argument = arguments.idMap.get( A_FILES );
			List<String> filesList = argument.values;
			taskFileListFeeder( filesList, this );
			System.out.println( "Skipped: " + skipped );
		}
		@Override
		public void process(File file) {
			ArcReader arcReader = null;
			WarcReader warcReader = null;
			int gzipEntries = 0;
			int arcRecords = 0;
			int arcErrors = 0;
			int warcRecords = 0;
			int warcErrors = 0;
			try {
				ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( file ), 8192 ), 16 );
				if ( GzipInputStream.isGziped( pbin ) ) {
					System.out.println( "Processing: " + file.getName() );
					GzipInputStream gzin = new GzipInputStream( pbin );
					GzipEntry entry;
					ByteCountingPushBackInputStream in;
					byte[] buffer = new byte[ 8192 ];
					int read;
					long offset = 0;
					while ( (entry = gzin.getNextEntry()) != null ) {
						in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzin.getEntryInputStream(), 8192 ), 16 );
						++gzipEntries;
						if ( gzipEntries == 1 ) {
							if ( ArcReaderFactory.isArcFile( in ) ) {
								arcReader = ArcReaderFactory.getReaderUncompressed();
								arcReader.setBlockDigestEnabled( true );
								arcReader.setPayloadDigestEnabled( true );
								ArcVersionBlock version = arcReader.getVersionBlock( in );
								if ( version != null ) {
								    ++arcRecords;
								    version.close();
									if (version.hasErrors()) {
										arcErrors += version.getValidationErrors().size();
									}
								}
							}
							else if ( WarcReaderFactory.isWarcFile( in ) ) {
								warcReader = WarcReaderFactory.getReaderUncompressed();
								warcReader.setBlockDigestEnabled( true );
								warcReader.setPayloadDigestEnabled( true );
							}
						}
						if ( arcReader != null ) {
							if ( gzipEntries > 1 ) {
								boolean b = true;
								while ( b ) {
									ArcRecord arcRecord = arcReader.getNextRecordFrom( in, offset );
									if ( arcRecord != null ) {
									    ++arcRecords;
									    arcRecord.close();
										if (arcRecord.hasErrors()) {
											arcErrors += arcRecord.getValidationErrors().size();
										}
									}
									else {
										b = false;
									}
								}
							}
						}
						else if ( warcReader != null ) {
							WarcRecord warcRecord;
							while ( (warcRecord = warcReader.getNextRecordFrom( in ) ) != null ) {
								++warcRecords;
								warcRecord.close();
								if (warcRecord.hasErrors()) {
									warcErrors += warcRecord.getValidationErrors().size();
								}
							}
						}
						else {
							while ( (read = in.read(buffer)) != -1 ) {
							}
						}
						in.close();
						offset = pbin.getConsumed();
					}
					if ( arcReader != null ) {
						arcReader.close();
					}
					if ( warcReader != null ) {
						warcReader.close();
					}
					gzin.close();
				}
				else if ( ArcReaderFactory.isArcFile( pbin ) ) {
					System.out.println( "Processing: " + file.getName() );
					arcReader = ArcReaderFactory.getReaderUncompressed( pbin );
					arcReader.setBlockDigestEnabled( true );
					arcReader.setPayloadDigestEnabled( true );
					ArcVersionBlock version = arcReader.getVersionBlock();
					if ( version != null ) {
					    ++arcRecords;
						boolean b = true;
						while ( b ) {
							ArcRecord arcRecord = arcReader.getNextRecord();
							if ( arcRecord != null ) {
							    ++arcRecords;
							    arcRecord.close();
								if (arcRecord.hasErrors()) {
									arcErrors += arcRecord.getValidationErrors().size();
								}
							}
							else {
								b = false;
							}
						}
					}
					arcReader.close();
				}
				else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
					System.out.println( "Processing: " + file.getName() );
					warcReader = WarcReaderFactory.getReader( pbin );
					warcReader.setBlockDigestEnabled( true );
					warcReader.setPayloadDigestEnabled( true );
					WarcRecord warcRecord;
					while ( (warcRecord = warcReader.getNextRecord()) != null ) {
						++warcRecords;
						warcRecord.close();
						if (warcRecord.hasErrors()) {
							warcErrors += warcRecord.getValidationErrors().size();
						}
					}
					warcReader.close();
				}
				else {
					++skipped;
				}
				pbin.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			if ( gzipEntries > 0 ) {
				System.out.println( ">GZip.Entries: " + gzipEntries );
			}
			if ( arcReader != null ) {
				System.out.println( ">Arc.isValid: " + arcReader.isCompliant() );
				System.out.println( ">Arc.Records: " + arcRecords );
				System.out.println( ">Arc.Errors: " + arcErrors );
			}
			if ( warcReader != null ) {
				System.out.println( ">Warc.isValid: " + warcReader.isCompliant() );
				System.out.println( ">Warc.Records: " + warcRecords );
				System.out.println( ">Warc.Errors: " + warcErrors );
			}
		}
	}

}
