package org.jwat.tools.tasks.test;

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

	public File srcFile;

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

	public void printResult(boolean bShowErrors, PrintStream validOutput, PrintStream invalidOutput, PrintStream exceptionsOutput) {
		PrintStream output;
		if ((bGzipReader && (bGzipIsComppliant == false || gzipErrors > 0 || gzipWarnings > 0))
				|| (bArcReader && (bArcIsCompliant == false || arcErrors > 0 || arcWarnings > 0))
				|| (bWarcReader && (bWarcIsCompliant == false || warcErrors > 0 || warcWarnings > 0))) {
			output = invalidOutput;
		} else {
			output = validOutput;
		}

		output.println( "#" );
		output.println( "# Summary of '" + file + "'" );
		output.println( "#" );
		if (bGzipReader) {
			output.println( "    GZip.isValid: " + bGzipIsComppliant );
			output.println( "    GZip.Entries: " + gzipEntries );
			output.println( "     GZip.Errors: " + gzipErrors );
			output.println( "   GZip.Warnings: " + gzipWarnings );
		}
		if (bArcReader) {
			output.println( "     Arc.isValid: " + bArcIsCompliant );
			output.println( "     Arc.Records: " + arcRecords );
			output.println( "      Arc.Errors: " + arcErrors );
			output.println( "    Arc.Warnings: " + arcWarnings );
		}
		if (bWarcReader) {
			output.println( "    Warc.isValid: " + bWarcIsCompliant );
			output.println( "    Warc.Records: " + warcRecords );
			output.println( "     Warc.Errors: " + warcErrors );
			output.println( "   Warc.Warnings: " + warcWarnings );
		}

		TestFileResultItemDiagnosis itemDiagnosis;
		if ( bShowErrors && rdList.size() > 0 ) {
			output.println( "#" );
			output.println( "# Detailed diagnosis report for '" + file + "'" );
			output.println( "#" );
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
					output.println( "Error in '" + file + "'" );
					output.println( "       Offset: " + itemDiagnosis.offset + " (0x" + (Long.toHexString(itemDiagnosis.offset)) + ")" );
					output.println( "  Record Type: " + typeStr );
					showDiagnosisList(itemDiagnosis.errors.iterator(), output);
				}
				if ( itemDiagnosis.warnings.size() > 0 ) {
					String typeStr = itemDiagnosis.type;
					if ( typeStr == null || typeStr.length() == 0 ) {
						typeStr = "unknown";
					}
					else {
						typeStr = "'" + typeStr + "'";
					}
					output.println( "Warning in '" + file + "'" );
					output.println( "       Offset: " + itemDiagnosis.offset + " (0x" + (Long.toHexString(itemDiagnosis.offset)) + ")" );
					output.println( "  Record Type: " + typeStr );
					showDiagnosisList(itemDiagnosis.warnings.iterator(), output);
				}
			}
		}

		// Report exceptions also in v.out

		runtimeErrors += throwableList.size();
		if ( throwableList.size() > 0 ) {
			Iterator<TestFileResultItemThrowable> iter = throwableList.iterator();
			TestFileResultItemThrowable itemThrowable;
			while ( iter.hasNext() ) {
				itemThrowable = iter.next();
				exceptionsOutput.println( "#" );
				exceptionsOutput.println( "# Exception while processing '" + file + "'" );
				exceptionsOutput.println( "#" );
				exceptionsOutput.println( "StartOffset: " + itemThrowable.startOffset + " (0x" + (Long.toHexString(itemThrowable.startOffset)) + ")" );
				exceptionsOutput.println( "     Offset: " + itemThrowable.offset + " (0x" + (Long.toHexString(itemThrowable.offset)) + ")" );
				itemThrowable.t.printStackTrace( exceptionsOutput );
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
			String[] labels = null;
			switch (diagnosis.type) {
			/*
			 * 0
			 */
			case EMPTY:
			case INVALID:
			case RECOMMENDED_MISSING:
			case REQUIRED_MISSING:
				labels = new String[0];
				break;
			/*
			 * 1
			 */
			case DUPLICATE:
			case INVALID_DATA:
			case RESERVED:
			case UNKNOWN:
				labels = new String[] {"        Value: "};
				break;
			case ERROR_EXPECTED:
				labels = new String[] {"     Expected: "};
				break;
			case ERROR:
				labels = new String[] {"  Description: "};
				break;
			case REQUIRED_INVALID:
			case UNDESIRED_DATA:
				labels = new String[] {"        Value: "};
				break;
			/*
			 * 2
			 */
			case INVALID_ENCODING:
				labels = new String[] {"        Value: ", "     Encoding: "};
				break;
			case INVALID_EXPECTED:
				labels = new String[] {"        Value: ", "     Expected: "};
				break;
			case RECOMMENDED:
				labels = new String[] {"  Recommended: ", "   Instead of: "};
				break;
			}
			if (diagnosis.information != null) {
				for (int i=0; i<diagnosis.information.length; ++i) {
					if (labels != null && i < labels.length) {
						out.println( labels[i] + diagnosis.information[i] );
					}
					else {
						out.println( "             : " + diagnosis.information[i] );
					}
				}
			}
		}
	}

}
