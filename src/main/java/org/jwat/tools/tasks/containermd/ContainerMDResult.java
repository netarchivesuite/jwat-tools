package org.jwat.tools.tasks.containermd;

import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.Diagnosis;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.tasks.ResultItemThrowable;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcRecord;

public class ContainerMDResult {

	/*
	 * File.
	 */
	public File srcFile;
	public String file;
	public long srcFileSize;
	public long srcFileUncompressedSize = 0;
	public String srcFileDigest;
	public long totalBytes = 0;
	public String originalFileName = "";

	public boolean bGzipReader = false;
	public boolean bGzipIsCompliant = false;
	public int gzipEntries = 0;
	public int gzipErrors = 0;
	public int gzipWarnings = 0;

	public boolean bArcReader = false;
	public boolean bArcIsCompliant = false;
	public String arcVersion = "";
	public String arcProfile = "";
	public String arcOriginCode = "";
	public boolean arcHasVersionBlockPayload = false;
	public int arcRecords = 0;
	public int arcErrors = 0;
	public int arcWarnings = 0;

	public boolean bWarcReader = false;
	public boolean bWarcIsCompliant = false;
	public String warcVersion = "";
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

	public List<ContainerMDResultItemDiagnosis> rdList = new LinkedList<ContainerMDResultItemDiagnosis>();

	public List<ResultItemThrowable> throwableList = new LinkedList<ResultItemThrowable>();

	public ContainerMDWrapper[] wraps = new ContainerMDWrapper[9];

	public ContainerMDResult() {
		for (int i = 0; i < wraps.length; i++) {
			wraps[i] = new ContainerMDWrapper();
		}
	}

	public void printResult(PrintStream output) {
		StringBuilder sb = new StringBuilder(5000);

		ContainerMDUtils.startElement(sb, "containerMD", new String[] { "xmlns:" + ContainerMDUtils.CONTAINER_PREFIX,
		        ContainerMDUtils.CONTAINER_URI }
		// , new String[]{"xmlns:xsi",
		// "http://www.w3.org/2001/XMLSchema-instance"}
		// , new String[]{"xsi:schemaLocation",
		// "http://bibnum.bnf.fr/ns/containerMD-v1 http://bibnum.bnf.fr/ns/containerMD-v1.xsd"}
		        );
		String now = ContainerMDUtils.ISO_DATE_FORMAT.format(new Date());
		ContainerMDUtils.startElement(sb, "container", new String[] { "creationDateTime", now });
		ContainerMDUtils.makeElement(sb, "fixity", "", new String[] { "size", Long.toString(srcFileSize) });
		ContainerMDUtils.makeElement(sb, "originalName", originalFileName);
		if (bArcReader) {
			ContainerMDUtils.startElement(sb, "formatDesignation");
			ContainerMDUtils.makeElement(sb, "formatName", "application/x-ia-arc");
			ContainerMDUtils.makeElement(sb, "formatVersion", arcVersion);
			ContainerMDUtils.endElement(sb, "formatDesignation");
		} else if (bWarcReader) {
			ContainerMDUtils.startElement(sb, "formatDesignation");
			ContainerMDUtils.makeElement(sb, "formatName", "application/warc");
			ContainerMDUtils.makeElement(sb, "formatVersion", warcVersion);
			ContainerMDUtils.endElement(sb, "formatDesignation");
		}

		if (bGzipReader) {
			ContainerMDUtils.makeElement(sb, "encoding", "", new String[] { "type", "compression" }, new String[] { "method",
			        "application/x-gzip" }, new String[] { "originalSize", Long.toString(srcFileUncompressedSize) });
		}
		// assessmentInformation
		ContainerMDUtils.startElement(sb, "assessmentInformation", new String[] { "agentName", "jwat-tool" }, new String[] {
		        "agentVersion", "0.5.6" });
		if (rdList.size() == 0 && throwableList.size() == 0) {
			ContainerMDUtils.makeElement(sb, "outcome", "Valid");
		} else {
			ContainerMDUtils.makeElement(sb, "outcome", "Not-valid");
			ContainerMDResultItemDiagnosis itemDiagnosis;
			if (rdList.size() > 0) {
				Iterator<ContainerMDResultItemDiagnosis> rectryIter = rdList.iterator();
				while (rectryIter.hasNext()) {
					itemDiagnosis = rectryIter.next();
					if (itemDiagnosis.errors.size() > 0) {
						String typeStr = itemDiagnosis.type;
						if (typeStr == null || typeStr.length() == 0) {
							typeStr = "unknown";
						} else {
							typeStr = "'" + typeStr + "'";
						}
						ContainerMDUtils.startElement(sb, "outcomeDetailNote");
						// sb.append(typeStr);
						showDiagnosisList(itemDiagnosis.errors.iterator(), sb);
						ContainerMDUtils.endElement(sb, "outcomeDetailNote");
					}
					if (itemDiagnosis.warnings.size() > 0) {
						String typeStr = itemDiagnosis.type;
						if (typeStr == null || typeStr.length() == 0) {
							typeStr = "unknown";
						} else {
							typeStr = "'" + typeStr + "'";
						}
						ContainerMDUtils.startElement(sb, "outcomeDetailNote");
						showDiagnosisList(itemDiagnosis.warnings.iterator(), sb);
						ContainerMDUtils.endElement(sb, "outcomeDetailNote");
					}
				}
			}
			runtimeErrors += throwableList.size();
			if (throwableList.size() > 0) {
				Iterator<ResultItemThrowable> iter = throwableList.iterator();
				ResultItemThrowable itemThrowable;
				while (iter.hasNext()) {
					itemThrowable = iter.next();
					ContainerMDUtils.startElement(sb,

					"outcomeDetailNote");
					StackTraceElement[] stes = itemThrowable.t.getStackTrace();
					for (StackTraceElement ste : stes) {
						sb.append(ContainerMDUtils.encodeContent(ste.toString())).append('\n');
					}
					ContainerMDUtils.endElement(sb, "outcomeDetailNote");
				}
			}
		}
		ContainerMDUtils.endElement(sb, "assessmentInformation");
		// containerExtension
		if (bArcReader) {
			ContainerMDUtils.startElement(sb, "containerExtension");
			ContainerMDUtils.startElement(sb, "ARCContainer");
			ContainerMDUtils.makeElement(sb, "fileName", originalFileName);
			ContainerMDUtils.makeElement(sb, "profile", arcProfile);
			ContainerMDUtils.makeElement(sb, "originCode", arcOriginCode);
			ContainerMDUtils.makeElement(sb, "versionBlockPayload", Boolean.toString(arcHasVersionBlockPayload));
			ContainerMDUtils.endElement(sb, "ARCContainer");
			ContainerMDUtils.endElement(sb, "containerExtension");
		} else if (bWarcReader) {
			// no WARCContainer
		}
		ContainerMDUtils.endElement(sb, "container"); // fin container

		// entries
		ContainerMDWrapper wrap = wraps[0];
		if (wrap != null) {
			ContainerMDUtils.startElement(sb, "entries"); // start
			                                              // entries
			ContainerMDUtils.startElement(sb, "entriesInformation", new String[] { "number", Long.toString(wrap.getNumber()) },
			        new String[] { "minimumSize", wrap.getMinimumSize() }, new String[] { "maximumSize", wrap.getMaximumSize() },
			        new String[] { "firstDateTime", wrap.getFirstDateTime() },
			        new String[] { "lastDateTime", wrap.getLastDateTime() },
			        new String[] { "globalSize", Long.toString(wrap.getGlobalSize()) });

			wrap.getPayloadFormats(sb, "format", false);
			wrap.getEncodings(sb);
			if (bArcReader) {
				ContainerMDUtils.startElement(sb, "entriesExtension");
				ContainerMDUtils.startElement(sb, "ARCEntries");
				wrap.getDeclaredMimeTypes(sb);
				wrap.getHosts(sb);
				wrap.getResponses(sb);
				ContainerMDUtils.endElement(sb, "ARCEntries");
				ContainerMDUtils.endElement(sb, "entriesExtension");
			} else if (bWarcReader) {
				ContainerMDUtils.startElement(sb, "entriesExtension");
				ContainerMDUtils.startElement(sb, "WARCEntries");

				for (int recordType = 1; recordType < wraps.length; recordType++) {
					ContainerMDWrapper lwrap = wraps[recordType];
					String name = WarcConstants.RT_IDX_STRINGS[recordType] + "Records";
					if (recordType == 1)
						name = "warcInfoRecords";

					if (lwrap != null && lwrap.getNumber() != 0) {
						ContainerMDUtils.startElement(sb, name, new String[] { "number", Long.toString(lwrap.getNumber()) },
						        new String[] { "minimumSize", lwrap.getMinimumSize() },
						        new String[] { "maximumSize", lwrap.getMaximumSize() },
						        new String[] { "firstDateTime", lwrap.getFirstDateTime() },
						        new String[] { "lastDateTime", lwrap.getLastDateTime() },
						        new String[] { "globalSize", Long.toString(lwrap.getGlobalSize()) });

						lwrap.getBlockFormats(sb, "blockFormat", true); // blockFormats
						lwrap.getPayloadFormats(sb, "payloadFormat", true); // payloadFormats
						if (recordType == WarcConstants.RT_IDX_RESOURCE || recordType == WarcConstants.RT_IDX_RESPONSE) {
							lwrap.getDeclaredMimeTypes(sb);
							lwrap.getHosts(sb);
							lwrap.getResponses(sb);
						}
						ContainerMDUtils.endElement(sb, name);
					}
				}
				ContainerMDUtils.endElement(sb, "WARCEntries");
				ContainerMDUtils.endElement(sb, "entriesExtension");
			}
			ContainerMDUtils.endElement(sb, "entriesInformation");
			ContainerMDUtils.endElement(sb, "entries");
		}
		ContainerMDUtils.endElement(sb, "containerMD"); // end containerMD

		// Pretty print XML
		// output.println(sb.toString());
		ContainerMDUtils.prettyPrintXml(sb.toString(), output);
	}

	public static void showGzipErrors(File file, GzipEntry gzipEntry, StringBuilder sb) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if (gzipEntry.diagnostics.hasErrors()) {
			sb.append("Error in '" + file.getPath() + "'");
			sb.append("       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset()))
			        + ")");
			diagnosisList = gzipEntry.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
		if (gzipEntry.diagnostics.hasWarnings()) {
			sb.append("Warning in '" + file.getPath() + "'");
			sb.append("       Offset: " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset()))
			        + ")");
			diagnosisList = gzipEntry.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
	}

	public static void showArcErrors(File file, ArcRecordBase arcRecord, StringBuilder sb) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if (arcRecord.diagnostics.hasErrors()) {
			sb.append("Error in '" + file.getPath() + "'");
			sb.append("       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset()))
			        + ")");
			diagnosisList = arcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
		if (arcRecord.diagnostics.hasWarnings()) {
			sb.append("Error in '" + file.getPath() + "'");
			sb.append("       Offset: " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset()))
			        + ")");
			diagnosisList = arcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
	}

	public static void showWarcErrors(File file, WarcRecord warcRecord, StringBuilder sb) {
		List<Diagnosis> diagnosisList;
		Iterator<Diagnosis> diagnosisIterator;
		if (warcRecord.diagnostics.hasErrors()) {
			String warcTypeStr = warcRecord.header.warcTypeStr;
			if (warcTypeStr == null || warcTypeStr.length() == 0) {
				warcTypeStr = "unknown";
			} else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			sb.append("Error in '" + file.getPath() + "'");
			sb.append("       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset()))
			        + ")");
			sb.append("  Record Type: " + warcTypeStr);
			diagnosisList = warcRecord.diagnostics.getErrors();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
		if (warcRecord.diagnostics.hasWarnings()) {
			String warcTypeStr = warcRecord.header.warcTypeStr;
			if (warcTypeStr == null || warcTypeStr.length() == 0) {
				warcTypeStr = "unknown";
			} else {
				warcTypeStr = "'" + warcTypeStr + "'";
			}
			sb.append("Warning in '" + file.getPath() + "'");
			sb.append("       Offset: " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset()))
			        + ")");
			sb.append("  Record Type: " + warcTypeStr);
			diagnosisList = warcRecord.diagnostics.getWarnings();
			diagnosisIterator = diagnosisList.iterator();
			showDiagnosisList(diagnosisIterator, sb);
		}
	}

	public static void showDiagnosisList(Iterator<Diagnosis> diagnosisIterator, StringBuilder sb) {
		Diagnosis diagnosis;
		boolean bFirst = true;
		while (diagnosisIterator.hasNext()) {
			diagnosis = diagnosisIterator.next();
			if (bFirst) {
				bFirst = false;
			} else {
				sb.append(", ");
			}
			sb.append("Type: ").append(diagnosis.type.name()).append(' ');
			// out.println("       Entity: " + diagnosis.entity);
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
				labels = new String[] { "Value: " };
				break;
			case ERROR_EXPECTED:
				labels = new String[] { "Expected: " };
				break;
			case ERROR:
				labels = new String[] { "Description: " };
				break;
			case REQUIRED_INVALID:
			case UNDESIRED_DATA:
				labels = new String[] { "Value: " };
				break; /*
					    * 2
					    */
			case INVALID_ENCODING:
				labels = new String[] { "Value: ", "Encoding: " };
				break;
			case INVALID_EXPECTED:
				labels = new String[] { "Value: ", "Expected: " };
				break;
			case RECOMMENDED:
				labels = new String[] { "Recommended: ", "Instead of: " };
				break;
			}
			if (diagnosis.information != null) {
				for (int i = 0; i < diagnosis.information.length; ++i) {
					if (i != 0) {
						sb.append(' ');
					}
					if (labels != null && i < labels.length) {
						sb.append(ContainerMDUtils.encodeContent(labels[i] + diagnosis.information[i]));
					} else {
						sb.append(ContainerMDUtils.encodeContent(" : " + diagnosis.information[i]));
					}
				}
			}
		}
	}

}
