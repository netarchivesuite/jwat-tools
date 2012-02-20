package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.arc.ArcValidationError;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.gzip.GzipReader;
import org.jwat.gzip.GzipReaderEntry;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcValidationError;

public class TestTask extends Task {

	private int arcGzFiles = 0;
	private int warcGzFiles = 0;
	private int gzFiles = 0;
	private int arcFiles = 0;
	private int warcFiles = 0;
	private int skipped = 0;

	private boolean bShowErrors = false;

	public TestTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
		}
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
		System.out.println( "-------" );
		System.out.println( "Summary" );
		System.out.println( "gzip arc files: " + arcGzFiles );
		System.out.println( "gzip warc files: " + warcGzFiles );
		System.out.println( "gzip files: " + gzFiles );
		System.out.println( "arc files: " + arcFiles );
		System.out.println( "warc files: " + warcFiles );
		System.out.println( "Skipped: " + skipped );
	}

	@Override
	public void process(File srcFile) {
		ByteCountingPushBackInputStream pbin = null;
		ArcReader arcReader = null;
		WarcReader warcReader = null;
		int gzipEntries = 0;
		int arcRecords = 0;
		int arcErrors = 0;
		int warcRecords = 0;
		int warcErrors = 0;
		try {
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( srcFile ), 8192 ), 16 );
			if ( GzipReader.isGzipped( pbin ) ) {
				System.out.println( "Processing: " + srcFile.getName() );
				GzipReader gzipReader = new GzipReader( pbin );
				GzipReaderEntry gzipEntry;
				ByteCountingPushBackInputStream in;
				byte[] buffer = new byte[ 8192 ];
				int read;
				long offset = 0;
				while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
					in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzipEntry.getInputStream(), 8192 ), 16 );
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
								if ( bShowErrors ) {
									showArcErrors( srcFile, version );
								}
							}
							++arcGzFiles;
						}
						else if ( WarcReaderFactory.isWarcFile( in ) ) {
							warcReader = WarcReaderFactory.getReaderUncompressed();
							warcReader.setBlockDigestEnabled( true );
							warcReader.setPayloadDigestEnabled( true );
							++warcGzFiles;
						}
						else {
							++gzFiles;
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
									if ( bShowErrors ) {
										showArcErrors( srcFile, arcRecord );
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
							if ( bShowErrors ) {
								showWarcErrors( srcFile, warcRecord );
							}
						}
					}
					else {
						while ( (read = in.read(buffer)) != -1 ) {
						}
					}
					in.close();
					gzipEntry.close();
					offset = pbin.getConsumed();
				}
				if ( arcReader != null ) {
					arcReader.close();
				}
				if ( warcReader != null ) {
					warcReader.close();
				}
				gzipReader.close();
			}
			else if ( ArcReaderFactory.isArcFile( pbin ) ) {
				System.out.println( "Processing: " + srcFile.getName() );
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
							if ( bShowErrors ) {
								showArcErrors( srcFile, arcRecord );
							}
						}
						else {
							b = false;
						}
					}
				}
				arcReader.close();
				++arcFiles;
			}
			else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
				System.out.println( "Processing: " + srcFile.getName() );
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
					if ( bShowErrors ) {
						showWarcErrors( srcFile, warcRecord );
					}
				}
				warcReader.close();
				++warcFiles;
			}
			else {
				++skipped;
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

	protected void showArcErrors(File file, ArcRecordBase arcRecord) {
		Collection<ArcValidationError> arcValidationErrors = arcRecord.getValidationErrors();
		if ( arcValidationErrors != null ) {
			System.out.println( "Error in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")" );
			Iterator<ArcValidationError> iter = arcValidationErrors.iterator();
			ArcValidationError arcError;
			while ( iter.hasNext() ) {
				arcError = iter.next();
				System.out.println( "   Error type: " + arcError.error );
				System.out.println( "   Field/Desc: " + arcError.field );
				System.out.println( "        Value: " + arcError.value );
			}
		}
	}

	protected void showWarcErrors(File file, WarcRecord warcRecord) {
		Collection<WarcValidationError> warcValidationErrors = warcRecord.getValidationErrors();
		if ( warcValidationErrors != null ) {
			String warcTypeStr = warcRecord.warcTypeStr;
			if ( warcTypeStr == null || warcTypeStr.length() == 0 ) {
				warcTypeStr = "unknown";
			}
			else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			System.out.println( "Error in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")" );
			System.out.println( "  Record Type: " + warcTypeStr );
			Iterator<WarcValidationError> iter = warcValidationErrors.iterator();
			WarcValidationError warcError;
			while ( iter.hasNext() ) {
				warcError = iter.next();
				System.out.println( "   Error type: " + warcError.error );
				System.out.println( "   Field/Desc: " + warcError.field );
				System.out.println( "        Value: " + warcError.value );
			}
		}
	}

}
