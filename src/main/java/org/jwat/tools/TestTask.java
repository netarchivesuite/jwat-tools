package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.Diagnosis;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class TestTask extends Task {

	private int arcGzFiles = 0;
	private int warcGzFiles = 0;
	private int gzFiles = 0;
	private int arcFiles = 0;
	private int warcFiles = 0;
	private int errors = 0;
	private int warnings = 0;
	private int runtimeErrors = 0;
	private int skipped = 0;

	private boolean bShowErrors = false;

	public TestTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		if ( arguments.idMap.containsKey( JWATTools.A_SHOW_ERRORS ) ) {
			bShowErrors = true;
		}
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
		System.out.println( "----------" );
		System.out.println( "Summary..." );
		System.out.println( "GZip files: " + gzFiles );
		System.out.println( "  +  Arc: " + arcGzFiles );
		System.out.println( "  + Warc: " + warcGzFiles );
		System.out.println( " Arc files: " + arcFiles );
		System.out.println( "Warc files: " + warcFiles );
		System.out.println( "    Errors: " + errors );
		System.out.println( "  Warnings: " + warnings );
		System.out.println( "RuntimeErr: " + runtimeErrors );
		System.out.println( "   Skipped: " + skipped );
	}

	@Override
	public void process(File srcFile) {
		ByteCountingPushBackInputStream pbin = null;
		GzipReader gzipReader = null;
		ArcReader arcReader = null;
		WarcReader warcReader = null;
		int gzipEntries = 0;
		int gzipErrors = 0;
		int gzipWarnings = 0;
		int arcRecords = 0;
		int arcErrors = 0;
		int arcWarnings = 0;
		int warcRecords = 0;
		int warcErrors = 0;
		int warcWarnings = 0;
		try {
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( srcFile ), 8192 ), 16 );
			if ( GzipReader.isGzipped( pbin ) ) {
				gzipReader = new GzipReader( pbin );
				GzipEntry gzipEntry;
				ByteCountingPushBackInputStream in;
				byte[] buffer = new byte[ 8192 ];
				int read;
				long offset = 0;
				while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
					in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzipEntry.getInputStream(), 8192 ), 16 );
					++gzipEntries;

					//System.out.println(gzipEntries + " - " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")");

					if ( gzipEntries == 1 ) {
						if ( ArcReaderFactory.isArcFile( in ) ) {
							arcReader = ArcReaderFactory.getReaderUncompressed();
							arcReader.setBlockDigestEnabled( true );
							arcReader.setPayloadDigestEnabled( true );
							ArcVersionBlock version = arcReader.getVersionBlockFrom( in, gzipEntry.getStartOffset() );
							if ( version != null ) {
							    ++arcRecords;
							    version.close();
								arcErrors += version.diagnostics.getErrors().size();
								arcWarnings += version.diagnostics.getWarnings().size();
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
								ArcRecord arcRecord = arcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() );
								if ( arcRecord != null ) {
								    ++arcRecords;
								    arcRecord.close();
									arcErrors += arcRecord.diagnostics.getErrors().size();
									arcWarnings += arcRecord.diagnostics.getWarnings().size();
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
						while ( (warcRecord = warcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() ) ) != null ) {
							++warcRecords;
							warcRecord.close();
							warcErrors += warcRecord.diagnostics.getErrors().size();
							warcWarnings += warcRecord.diagnostics.getWarnings().size();
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
					gzipErrors = gzipEntry.diagnostics.getErrors().size();
					gzipWarnings = gzipEntry.diagnostics.getWarnings().size();
					if ( bShowErrors ) {
						showGzipErrors(srcFile, gzipEntry);
					}
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

							//System.out.println(arcRecords + " - " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")");

							arcRecord.close();
							arcErrors += arcRecord.diagnostics.getErrors().size();
							arcWarnings += arcRecord.diagnostics.getWarnings().size();
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
				warcReader = WarcReaderFactory.getReader( pbin );
				warcReader.setBlockDigestEnabled( true );
				warcReader.setPayloadDigestEnabled( true );
				WarcRecord warcRecord;
				while ( (warcRecord = warcReader.getNextRecord()) != null ) {
					++warcRecords;

					//System.out.println(warcRecords + " - " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")");

					warcRecord.close();
					warcErrors += warcRecord.diagnostics.getErrors().size();
					warcWarnings += warcRecord.diagnostics.getWarnings().size();
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
		catch (Throwable t) {
			++runtimeErrors;
			t.printStackTrace();
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
		if (gzipReader != null || arcReader != null || warcReader != null) {
			System.out.println( "Summary of '" + srcFile.getPath() + "'" );
			if ( gzipEntries > 0 ) {
				//System.out.println( "    GZip.isValid: " + gzipReader.isCompliant() );
				System.out.println( "    GZip.Entries: " + gzipEntries );
				System.out.println( "     GZip.Errors: " + gzipErrors );
				System.out.println( "   GZip.Warnings: " + gzipWarnings );
			}
			if ( arcReader != null ) {
				System.out.println( "     Arc.isValid: " + arcReader.isCompliant() );
				System.out.println( "     Arc.Records: " + arcRecords );
				System.out.println( "      Arc.Errors: " + arcErrors );
				System.out.println( "    Arc.Warnings: " + arcWarnings );
			}
			if ( warcReader != null ) {
				System.out.println( "    Warc.isValid: " + warcReader.isCompliant() );
				System.out.println( "    Warc.Records: " + warcRecords );
				System.out.println( "     Warc.Errors: " + warcErrors );
				System.out.println( "   Warc.Warnings: " + warcWarnings );
			}
		}
		errors += gzipErrors;
		warnings += gzipWarnings;
		errors += arcErrors;
		warnings +=arcWarnings;
		errors += warcErrors;
		warnings += warcWarnings;
	}

	protected void showGzipErrors(File file, GzipEntry gzipEntry) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( gzipEntry.diagnostics.hasErrors() ) {
			System.out.println( "Error in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")" );
			diagnosisList = gzipEntry.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
		if ( gzipEntry.diagnostics.hasWarnings() ) {
			System.out.println( "Warning in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")" );
			diagnosisList = gzipEntry.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
	}

	protected void showArcErrors(File file, ArcRecordBase arcRecord) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( arcRecord.diagnostics.hasErrors() ) {
			System.out.println( "Error in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")" );
			diagnosisList = arcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
		if ( arcRecord.diagnostics.hasWarnings() ) {
			System.out.println( "Error in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")" );
			diagnosisList = arcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
	}

	protected void showWarcErrors(File file, WarcRecord warcRecord) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( warcRecord.diagnostics.hasErrors() ) {
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
			diagnosisList = warcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
		if ( warcRecord.diagnostics.hasWarnings() ) {
			String warcTypeStr = warcRecord.warcTypeStr;
			if ( warcTypeStr == null || warcTypeStr.length() == 0 ) {
				warcTypeStr = "unknown";
			}
			else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			System.out.println( "Warning in '" + file.getPath() + "'" );
			System.out.println( "       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")" );
			System.out.println( "  Record Type: " + warcTypeStr );
			diagnosisList = warcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator);
		}
	}

	protected void showDiagnosisList(Iterator<Diagnosis> diagnosisIterator) {
		Diagnosis diagnosis;
		while (diagnosisIterator.hasNext()) {
			diagnosis = diagnosisIterator.next();
			System.out.println( "         Type: " + diagnosis.type.name() );
			System.out.println( "       Entity: " + diagnosis.entity );
			switch (diagnosis.type) {
			case EMPTY:
			case INVALID:
				break;
			case RECOMMENDED:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						System.out.println( "  Recommended: " + diagnosis.information[0] );
					}
					if (diagnosis.information.length >= 2) {
						System.out.println( "   Instead of: " + diagnosis.information[1] );
					}
				}
				break;
			case REQUIRED_INVALID:
			case UNDESIRED_DATA:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						System.out.println( "        Value: " + diagnosis.information[0] );
					}
				}
				break;
			case DUPLICATE:
			case RESERVED:
			case UNKNOWN:
			case INVALID_DATA:
				System.out.println( "        Value: " + diagnosis.information[0] );
				break;
			case INVALID_ENCODING:
				System.out.println( "        Value: " + diagnosis.information[0] );
				System.out.println( "     Encoding: " + diagnosis.information[1] );
				break;
			case INVALID_EXPECTED:
				System.out.println( "        Value: " + diagnosis.information[0] );
			case ERROR_EXPECTED:
				System.out.println( "     Expected: " + diagnosis.information[1] );
				break;
			}
		}
	}

}
