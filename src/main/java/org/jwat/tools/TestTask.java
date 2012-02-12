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
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipInputStream;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcValidationError;

public class TestTask extends Task {

	private int skipped = 0;

	private boolean bShowErrors = false;

	public TestTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
		}
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
								if ( bShowErrors ) {
									showArcErrors( version );
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
									if ( bShowErrors ) {
										showArcErrors( arcRecord );
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
								showWarcErrors( warcRecord );
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
							if ( bShowErrors ) {
								showArcErrors( arcRecord );
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
					if ( bShowErrors ) {
						showWarcErrors( warcRecord );
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

	protected void showArcErrors(ArcRecordBase arcRecord) {
		Collection<ArcValidationError> arcValidationErrors = arcRecord.getValidationErrors();
		if ( arcValidationErrors != null ) {
			Iterator<ArcValidationError> iter = arcValidationErrors.iterator();
			ArcValidationError arcError;
			while ( iter.hasNext() ) {
				arcError = iter.next();
				System.out.println( arcRecord.getOffset() );
				System.out.println( " Error - t: " + arcError.error + " - f: " + arcError.field + " - v: " + arcError.value );
			}
		}
	}

	protected void showWarcErrors(WarcRecord warcRecord) {
		Collection<WarcValidationError> warcValidationErrors = warcRecord.getValidationErrors();
		if ( warcValidationErrors != null ) {
			Iterator<WarcValidationError> iter = warcValidationErrors.iterator();
			WarcValidationError warcError;
			while ( iter.hasNext() ) {
				warcError = iter.next();
				String warcTypeStr = warcRecord.warcTypeStr;
				if ( warcTypeStr == null || warcTypeStr.length() == 0 ) {
					warcTypeStr = "unknown";
				}
				else {
					warcTypeStr = "'" + warcTypeStr + "'";
				}
				System.out.println( "Error in " + warcTypeStr + " record at offset: " + warcRecord.getOffset() + " (0x" + (Long.toHexString(warcRecord.getOffset())) + ")" );
				System.out.println( "\tError type: " + warcError.error );
				System.out.println( "\tField/Desc: " + warcError.field );
				System.out.println( "\t     Value: " + warcError.value );
			}
		}
	}

}
