package org.jwat.tools.tasks.containermd;

import java.io.File;
import java.io.IOException;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ArchiveParser;
import org.jwat.archive.ArchiveParserCallback;
import org.jwat.archive.FileIdent;
import org.jwat.archive.ManagedPayload;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.tools.core.ManagedPayloadContentType;
import org.jwat.tools.core.ManagedPayloadContentTypeIdentifier;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcRecord;

public class ParseContainerMD implements ArchiveParserCallback {

	private static final String EMPTY_MIME_TYPE = "application/x-empty";

	private ContainerMDOptions options;

	protected ManagedPayload managedPayload;

	public ContainerMDUpdateCallback callback;

	protected ContainerMDResult result;

	public ContainerMDResult processFile(File file, ContainerMDOptions options) {
		this.options = options;

		if (!options.bQuiet) System.out.println("processFile " + file.getAbsolutePath());

		result = new ContainerMDResult();
		result.file = file.getPath();

		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = options.uriProfile;
		archiveParser.bBlockDigestEnabled = false;
		archiveParser.bPayloadDigestEnabled = false;
		archiveParser.recordHeaderMaxSize = options.recordHeaderMaxSize;
		archiveParser.payloadHeaderMaxSize = options.payloadHeaderMaxSize;

		managedPayload = ManagedPayload.checkout();

		long consumed = archiveParser.parse(file, this);

		managedPayload.checkin();

		result.bGzipReader = (archiveParser.gzipReader != null);
		result.bArcReader = (archiveParser.arcReader != null);
		result.bWarcReader = (archiveParser.warcReader != null);
		if (archiveParser.gzipReader != null) {
			result.bGzipIsCompliant = archiveParser.gzipReader.isCompliant();
			result.wraps[0].handleEncoding("compression", "Deflate");
		}
		if (archiveParser.arcReader != null) {
			result.bArcIsCompliant = archiveParser.arcReader.isCompliant();
		}
		if (archiveParser.warcReader != null) {
			result.bWarcIsCompliant = archiveParser.warcReader.isCompliant();
		}

		if (callback != null) {
			callback.finalUpdate(result, consumed);
		}
		return result;
	}

	@Override
	public void apcFileId(File file, int fileId) {
		switch (fileId) {
		case FileIdent.FILEID_GZIP:
			++result.gzFiles;
			break;
		case FileIdent.FILEID_ARC:
			++result.arcFiles;
			break;
		case FileIdent.FILEID_WARC:
			++result.warcFiles;
			break;
		case FileIdent.FILEID_ARC_GZ:
			++result.arcGzFiles;
			break;
		case FileIdent.FILEID_WARC_GZ:
			++result.warcGzFiles;
			break;
		case FileIdent.FILEID_UNKNOWN:
			++result.skipped;
			break;
		}
	}

	@Override
	public void apcGzipEntryStart(GzipEntry gzipEntry, long startOffset) {
		++result.gzipEntries;
		result.srcFileUncompressedSize += gzipEntry.uncompressed_size;
		
		/*
		System.out.println("apcGzipEntryStart " + result.gzipEntries + " - (0x"
				+ (Long.toHexString(startOffset)) + "-" + startOffset + "-" + gzipEntry.compressed_size + "-" + gzipEntry.uncompressed_size +  ")");
		*/
		result.gzipErrors = gzipEntry.diagnostics.getErrors().size();
		result.gzipWarnings = gzipEntry.diagnostics.getWarnings().size();

		if (gzipEntry.diagnostics.hasErrors()
				|| gzipEntry.diagnostics.hasWarnings()) {
			ContainerMDResultItemDiagnosis itemDiagnosis = new ContainerMDResultItemDiagnosis();
			itemDiagnosis.offset = startOffset;
			itemDiagnosis.errors = gzipEntry.diagnostics.getErrors();
			itemDiagnosis.warnings = gzipEntry.diagnostics.getWarnings();
			result.rdList.add(itemDiagnosis);
		}
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset,
			boolean compressed) throws IOException {
		++result.arcRecords;
		if (!options.bQuiet && (result.arcRecords % 10 == 0)) {
			System.out.println("apcArcRecordStart " + result.arcRecords + " - "
					+ arcRecord.getStartOffset() + " (0x"
					+ (Long.toHexString(arcRecord.getStartOffset())) + ") " 
					+ arcRecord.getUrlStr() + " " + arcRecord.getArchiveLength());
		}
		ContainerMDResultItemDiagnosis itemDiagnosis = new ContainerMDResultItemDiagnosis();
		itemDiagnosis.offset = startOffset;
		switch (arcRecord.recordType) {
		case ArcRecordBase.RT_VERSION_BLOCK:
			managedPayload.manageVersionBlock(arcRecord, false);
			result.arcVersion = Integer.toString(arcRecord.versionHeader.version.major);
			result.arcProfile = arcRecord.versionHeader.versionStr;
			String filedesc = arcRecord.header.urlStr;
			if (filedesc != null && filedesc.startsWith("filedesc://")) {
				result.originalFileName = filedesc.substring("filedesc://".length());
			}
			result.arcOriginCode = arcRecord.versionHeader.originCode;
			--result.arcRecords; // Don't count the version block as a record 
			break;
		case ArcRecordBase.RT_ARC_RECORD:
			// better look inside the ARC record
			managedPayload.manageArcRecord(arcRecord, false);
			break;
		default:
			throw new IllegalStateException();
		}
		int recordType = arcRecord.recordType;
		String recordUrl = arcRecord.getUrlStr();
		long recordLength = arcRecord.getArchiveLength();
		String recordDate = arcRecord.getArchiveDateStr();
		String recordProvidedContentType = arcRecord.getContentTypeStr();
		HttpHeader httpHeader = arcRecord.getHttpHeader();
		String recordProtocol = "";
		String recordStatusCode = "";
		if (httpHeader != null && httpHeader.isValid()) {
			recordProtocol =  httpHeader.getProtocolVersion();
			recordStatusCode = httpHeader.statusCodeStr;
		}
		if (arcRecord.diagnostics.hasErrors()
				|| arcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
		}

		String recordContentTypeStr = null;
		if (arcRecord.hasPayload()) {
			ContentType recordContentType = ContentType.parseContentType("application/octet-stream");
			ManagedPayloadContentTypeIdentifier managedPayloadContentTypeIdentifier = ManagedPayloadContentTypeIdentifier
					.getManagedPayloadContentTypeIdentifier();
			if (managedPayloadContentTypeIdentifier != null) {
				ManagedPayloadContentType managedPayloadContentType = managedPayloadContentTypeIdentifier
						.estimateContentType(managedPayload);
				if (managedPayloadContentType != null) {
					recordContentType = managedPayloadContentType.contentType;
				} else {
					// System.err.println("BAD ARCRECORD " + recordUrl + " " + recordLength);
				}
			} else {
				// System.err.println("BAD ARCRECORD " + recordUrl + " " + recordLength);
			}
			recordContentTypeStr = recordContentType.toStringShort();
		} else {
			recordContentTypeStr = EMPTY_MIME_TYPE;
		}
		if (itemDiagnosis.errors.size() > 0
				|| itemDiagnosis.warnings.size() > 0) {
			result.rdList.add(itemDiagnosis);
		}
		if (recordType == ArcRecordBase.RT_ARC_RECORD) {
			if (EMPTY_MIME_TYPE.equals(recordContentTypeStr) && arcRecord.hasPayload()) {
				recordContentTypeStr = "text/plain";
			}

			result.wraps[0].addEntry(recordUrl, recordLength, recordDate,
					 recordProvidedContentType, recordProvidedContentType, recordContentTypeStr, 
					recordProtocol, recordStatusCode);
		} else if (recordType == ArcRecordBase.RT_VERSION_BLOCK) {
			result.arcHasVersionBlockPayload = arcRecord.hasPayload();
		}
		result.arcErrors += itemDiagnosis.errors.size();
		result.arcWarnings += itemDiagnosis.warnings.size();
		for (int i = 0; i < itemDiagnosis.throwables.size(); ++i) {
			ContainerMDResultItemThrowable itemThrowable = new ContainerMDResultItemThrowable();
			itemThrowable.startOffset = startOffset;
			itemThrowable.offset = startOffset;
			itemThrowable.t = itemDiagnosis.throwables.get(i);
			result.throwableList.add(itemThrowable);
		}
		
		// Don't forget to close the record
		managedPayload.close();
		arcRecord.close();
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset,
			boolean compressed) throws IOException {
		++result.warcRecords;
		if (!options.bQuiet && (result.warcRecords % 10 == 0)) {
			System.out.println("apcWarcRecordStart " + result.warcRecords + " - "
					+ warcRecord.getStartOffset() + " (0x"
					+ (Long.toHexString(warcRecord.getStartOffset())) + ")");
		}
		ContainerMDResultItemDiagnosis itemDiagnosis = new ContainerMDResultItemDiagnosis();
		itemDiagnosis.offset = startOffset;
		itemDiagnosis.type = warcRecord.header.warcTypeStr;

		managedPayload.manageWarcRecord(warcRecord, false);

		WarcHeader header = warcRecord.header;
		if (result.warcVersion.length() == 0) {
			result.warcVersion = header.versionStr;
		}
		int recordType = header.warcTypeIdx;
		if (recordType == WarcConstants.RT_IDX_WARCINFO) {
			result.originalFileName = header.warcFilename;
			
		}
		String recordUrl = header.warcTargetUriStr;
		long recordLength = header.contentLength;
		String recordDate = header.warcDateStr;
		String recordBlockProvidedContentType = header.contentTypeStr;
		if (recordBlockProvidedContentType == null && header.contentLength == 0) {
			recordBlockProvidedContentType =  EMPTY_MIME_TYPE;
		}
		
		HttpHeader httpHeader = warcRecord.getHttpHeader();
		String recordProtocol = "";
		String recordStatusCode = "";
		String httpDeclaredMimeType = "";
		if (httpHeader != null && httpHeader.isValid()) {
			recordProtocol =  httpHeader.getProtocolVersion();
			recordStatusCode = httpHeader.statusCodeStr;
			httpDeclaredMimeType = httpHeader.contentType;
		}
		if (warcRecord.diagnostics.hasErrors()
				|| warcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
		}
		
		String recordContentTypeStr = null;
		if (warcRecord.hasPayload()) {
			ContentType recordContentType = ContentType.parseContentType("application/octet-stream");
			ManagedPayloadContentTypeIdentifier managedPayloadContentTypeIdentifier = ManagedPayloadContentTypeIdentifier
					.getManagedPayloadContentTypeIdentifier();
			if (managedPayloadContentTypeIdentifier != null) {
				ManagedPayloadContentType managedPayloadContentType = managedPayloadContentTypeIdentifier
						.estimateContentType(managedPayload);
				if (managedPayloadContentType != null) {
					recordContentType = managedPayloadContentType.contentType;
				}
			}
			recordContentTypeStr = recordContentType.toStringShort();
		} else {
			recordContentTypeStr = EMPTY_MIME_TYPE;
		}
		
		if (EMPTY_MIME_TYPE.equals(recordContentTypeStr) && warcRecord.hasPayload()) {
			if (!options.bQuiet) System.err.println("BAD IDENTIFICATION for " + recordUrl + " l=" + recordLength);
			recordContentTypeStr = "text/plain";
		}
		
		result.wraps[0].addEntry(recordUrl, recordLength, recordDate,
				recordBlockProvidedContentType, httpDeclaredMimeType, recordContentTypeStr,
				recordProtocol, recordStatusCode);
		result.wraps[recordType].addEntry(recordUrl, recordLength, recordDate,
				recordBlockProvidedContentType, httpDeclaredMimeType, recordContentTypeStr,
				recordProtocol, recordStatusCode);

		if (itemDiagnosis.errors.size() > 0 || itemDiagnosis.warnings.size() > 0) {
			result.rdList.add(itemDiagnosis);
		}

		result.warcErrors += itemDiagnosis.errors.size();
		result.warcWarnings += itemDiagnosis.warnings.size();
		for (int i = 0; i < itemDiagnosis.throwables.size(); ++i) {
			ContainerMDResultItemThrowable itemThrowable = new ContainerMDResultItemThrowable();
			itemThrowable.startOffset = startOffset;
			itemThrowable.offset = startOffset;
			itemThrowable.t = itemDiagnosis.throwables.get(i);
			result.throwableList.add(itemThrowable);
		}
		
		// Don't forget to close the record
		managedPayload.close();
		warcRecord.close();
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
		if (callback != null) {
			callback.update(result, consumed);
		}
	}

	@Override
	public void apcRuntimeError(Throwable t, long startOffset, long consumed) {
		ContainerMDResultItemThrowable itemThrowable = new ContainerMDResultItemThrowable();
		itemThrowable.startOffset = startOffset;
		itemThrowable.offset = consumed;
		itemThrowable.t = t;
		result.throwableList.add(itemThrowable);
	}

	@Override
	public void apcDone(GzipReader gzipReader, ArcReader arcReader, WarcReader warcReader) {
	}
}
