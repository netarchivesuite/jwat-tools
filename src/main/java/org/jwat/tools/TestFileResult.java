package org.jwat.tools;

import java.io.File;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.Diagnosis;
import org.jwat.gzip.GzipEntry;
import org.jwat.warc.WarcRecord;

public class TestFileResult {

	/*
	 * File.
	 */

	public String file;
	public boolean bGzipReader = false;
	public boolean bGzipIsComppliant = false;
	public int gzipEntries = 0;
	public int gzipErrors = 0;
	public int gzipWarnings = 0;
	public boolean bArcReader = false;
	public boolean bArcIsCompliant = false;
	public int arcRecords = 0;
	public int arcErrors = 0;
	public int arcWarnings = 0;
	public boolean bWarcReader = false;
	public boolean bWarcIsCompliant = false;
	public int warcRecords = 0;
	public int warcErrors = 0;
	public int warcWarnings = 0;

	/*
	 * Summary.
	 */

	public int arcGzFiles = 0;
	public int warcGzFiles = 0;
	public int gzFiles = 0;
	public int arcFiles = 0;
	public int warcFiles = 0;
	public int runtimeErrors = 0;
	public int skipped = 0;

	public List<TestFileResultItemDiagnosis> rdList = new LinkedList<TestFileResultItemDiagnosis>();

	public List<TestFileResultItemThrowable> throwableList = new LinkedList<TestFileResultItemThrowable>();

	public void printResult(boolean bShowErrors, PrintStream out, PrintStream err) {
		out.println( "#" );
		out.println( "# Summary of '" + file + "'" );
		out.println( "#" );
		if (bGzipReader) {
			//out.println( "    GZip.isValid: " + gzipReader.isCompliant() );
			out.println( "    GZip.Entries: " + gzipEntries );
			out.println( "     GZip.Errors: " + gzipErrors );
			out.println( "   GZip.Warnings: " + gzipWarnings );
		}
		if (bArcReader) {
			out.println( "     Arc.isValid: " + bArcIsCompliant );
			out.println( "     Arc.Records: " + arcRecords );
			out.println( "      Arc.Errors: " + arcErrors );
			out.println( "    Arc.Warnings: " + arcWarnings );
		}
		if (bWarcReader) {
			out.println( "    Warc.isValid: " + bWarcIsCompliant );
			out.println( "    Warc.Records: " + warcRecords );
			out.println( "     Warc.Errors: " + warcErrors );
			out.println( "   Warc.Warnings: " + warcWarnings );
		}

		TestFileResultItemDiagnosis itemDiagnosis;
		if ( bShowErrors && rdList.size() > 0 ) {
			out.println( "#" );
			out.println( "# Detailed diagnosis report for '" + file + "'" );
			out.println( "#" );
			Iterator<TestFileResultItemDiagnosis> rectryIter = rdList.iterator();
			while (rectryIter.hasNext()) {
				itemDiagnosis = rectryIter.next();
				if ( itemDiagnosis.errors.size() > 0 ) {
					String typeStr = itemDiagnosis.type;
					if ( typeStr == null || typeStr.length() == 0 ) {
						typeStr = "unknown";
					}
					else {
						typeStr = "'" + typeStr + "'";
					}
					out.println( "Error in '" + file + "'" );
					out.println( "       Offset: " + itemDiagnosis.offset + " (0x" + (Long.toHexString(itemDiagnosis.offset)) + ")" );
					out.println( "  Record Type: " + typeStr );
					showDiagnosisList(itemDiagnosis.errors.iterator(), out);
				}
				if ( itemDiagnosis.warnings.size() > 0 ) {
					String typeStr = itemDiagnosis.type;
					if ( typeStr == null || typeStr.length() == 0 ) {
						typeStr = "unknown";
					}
					else {
						typeStr = "'" + typeStr + "'";
					}
					out.println( "Warning in '" + file + "'" );
					out.println( "       Offset: " + itemDiagnosis.offset + " (0x" + (Long.toHexString(itemDiagnosis.offset)) + ")" );
					out.println( "  Record Type: " + typeStr );
					showDiagnosisList(itemDiagnosis.warnings.iterator(), out);
				}
			}
		}

		if ( throwableList.size() > 0 ) {
			Iterator<TestFileResultItemThrowable> iter = throwableList.iterator();
			TestFileResultItemThrowable itemThrowable;
			while ( iter.hasNext() ) {
				itemThrowable = iter.next();
				err.println( "#" );
				err.println( "# Exception while processing '" + file + "'" );
				err.println( "#" );
				err.println( "StartOffset: " + itemThrowable.startOffset + " (0x" + (Long.toHexString(itemThrowable.startOffset)) + ")" );
				err.println( "     Offset: " + itemThrowable.offset + " (0x" + (Long.toHexString(itemThrowable.offset)) + ")" );
				itemThrowable.t.printStackTrace( err );
			}
		}
	}

	public static void showGzipErrors(File file, GzipEntry gzipEntry, PrintStream out) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( gzipEntry.diagnostics.hasErrors() ) {
			out.println( "Error in '" + file.getPath() + "'" );
			out.println( "       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")" );
			diagnosisList = gzipEntry.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
		if ( gzipEntry.diagnostics.hasWarnings() ) {
			out.println( "Warning in '" + file.getPath() + "'" );
			out.println( "       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")" );
			diagnosisList = gzipEntry.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
	}

	public static void showArcErrors(File file, ArcRecordBase arcRecord, PrintStream out) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( arcRecord.diagnostics.hasErrors() ) {
			out.println( "Error in '" + file.getPath() + "'" );
			out.println( "       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")" );
			diagnosisList = arcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
		if ( arcRecord.diagnostics.hasWarnings() ) {
			out.println( "Error in '" + file.getPath() + "'" );
			out.println( "       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")" );
			diagnosisList = arcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
	}

	public static void showWarcErrors(File file, WarcRecord warcRecord, PrintStream out) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if ( warcRecord.diagnostics.hasErrors() ) {
			String warcTypeStr = warcRecord.header.warcTypeStr;
			if ( warcTypeStr == null || warcTypeStr.length() == 0 ) {
				warcTypeStr = "unknown";
			}
			else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			out.println( "Error in '" + file.getPath() + "'" );
			out.println( "       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")" );
			out.println( "  Record Type: " + warcTypeStr );
			diagnosisList = warcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
		if ( warcRecord.diagnostics.hasWarnings() ) {
			String warcTypeStr = warcRecord.header.warcTypeStr;
			if ( warcTypeStr == null || warcTypeStr.length() == 0 ) {
				warcTypeStr = "unknown";
			}
			else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			out.println( "Warning in '" + file.getPath() + "'" );
			out.println( "       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")" );
			out.println( "  Record Type: " + warcTypeStr );
			diagnosisList = warcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, out);
		}
	}

	public static void showDiagnosisList(Iterator<Diagnosis> diagnosisIterator, PrintStream out) {
		Diagnosis diagnosis;
		while (diagnosisIterator.hasNext()) {
			diagnosis = diagnosisIterator.next();
			out.println( "         Type: " + diagnosis.type.name() );
			out.println( "       Entity: " + diagnosis.entity );
			switch (diagnosis.type) {
			/*
			 * 0
			 */
			case EMPTY:
			case INVALID:
			case RECOMMENDED_MISSING:
			case REQUIRED_MISSING:
				break;
			/*
			 * 1
			 */
			case DUPLICATE:
			case INVALID_DATA:
			case RESERVED:
			case UNKNOWN:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "        Value: " + diagnosis.information[0] );
					}
				}
				break;
			case ERROR_EXPECTED:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "     Expected: " + diagnosis.information[0] );
					}
				}
				break;
			case ERROR:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "  Description: " + diagnosis.information[0] );
					}
				}
				break;
			case REQUIRED_INVALID:
			case UNDESIRED_DATA:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "        Value: " + diagnosis.information[0] );
					}
				}
				break;
			/*
			 * 2
			 */
			case INVALID_ENCODING:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "        Value: " + diagnosis.information[0] );
					}
					if (diagnosis.information.length >= 2) {
						out.println( "     Encoding: " + diagnosis.information[1] );
					}
				}
				break;
			case INVALID_EXPECTED:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "        Value: " + diagnosis.information[0] );
					}
					if (diagnosis.information.length >= 2) {
						out.println( "     Expected: " + diagnosis.information[1] );
					}
				}
				break;
			case RECOMMENDED:
				if (diagnosis.information != null) {
					if (diagnosis.information.length >= 1) {
						out.println( "  Recommended: " + diagnosis.information[0] );
					}
					if (diagnosis.information.length >= 2) {
						out.println( "   Instead of: " + diagnosis.information[1] );
					}
				}
				break;
			}
		}
	}

}
