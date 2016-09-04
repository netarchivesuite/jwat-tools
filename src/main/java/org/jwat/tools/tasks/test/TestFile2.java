package org.jwat.tools.tasks.test;

import java.io.File;
import java.io.IOException;

import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ArchiveParser;
import org.jwat.archive.ArchiveParserCallback;
import org.jwat.archive.Cloner;
import org.jwat.archive.FileIdent;
import org.jwat.archive.ManagedPayload;
import org.jwat.common.ContentType;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ManagedPayloadContentTypeIdentifier;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.warc.WarcRecord;

public class TestFile2 implements ArchiveParserCallback {

	public TestOptions options;

	public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;

	protected ManagedPayload managedPayload;

	protected ManagedPayloadContentTypeIdentifier managedPayloadContentTypeIdentifier;

	protected Cloner cloner;

	public TestFileUpdateCallback callback;

	protected TestFileResult result;

	public TestFileResult processFile(File file, TestOptions options, Cloner cloner) {
		this.options = options;
		result = new TestFileResult();
		result.file = file.getPath();

		managedPayloadContentTypeIdentifier = ManagedPayloadContentTypeIdentifier.getManagedPayloadContentTypeIdentifier();

		//this.cloner = cloner;

		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = options.uriProfile;
		archiveParser.bBlockDigestEnabled = options.bValidateDigest;
		archiveParser.bPayloadDigestEnabled = options.bValidateDigest;
	    archiveParser.recordHeaderMaxSize = recordHeaderMaxSize;
	    archiveParser.payloadHeaderMaxSize = payloadHeaderMaxSize;

		managedPayload = ManagedPayload.checkout();

		long consumed = archiveParser.parse(file, this);

		managedPayload.checkin();

		result.bGzipReader = archiveParser.gzipReader != null;
		result.bArcReader = archiveParser.arcReader != null;
		result.bWarcReader = archiveParser.warcReader != null;
		if (archiveParser.gzipReader != null) {
			result.bGzipIsComppliant = archiveParser.gzipReader.isCompliant();
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
		result.gzipErrors = gzipEntry.diagnostics.getErrors().size();
		result.gzipWarnings = gzipEntry.diagnostics.getWarnings().size();
		if ( options.bShowErrors ) {
			//TestResult.showGzipErrors(srcFile, gzipEntry, System.out);
			if (gzipEntry.diagnostics.hasErrors() || gzipEntry.diagnostics.hasWarnings()) {
				TestFileResultItemDiagnosis itemDiagnosis = new TestFileResultItemDiagnosis();
				itemDiagnosis.offset = startOffset;
				itemDiagnosis.errors = gzipEntry.diagnostics.getErrors();
				itemDiagnosis.warnings = gzipEntry.diagnostics.getWarnings();
				result.rdList.add(itemDiagnosis);
			}
		}
	}

	@Override
	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException {
		++result.arcRecords;
		//System.out.println(arcRecords + " - " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")");
		TestFileResultItemDiagnosis itemDiagnosis = new TestFileResultItemDiagnosis();
		itemDiagnosis.offset = startOffset;
		// TODO arc type string in JWAT.
		switch (arcRecord.recordType) {
		case ArcRecordBase.RT_VERSION_BLOCK:
			managedPayload.manageVersionBlock(arcRecord, false);
			break;
		case ArcRecordBase.RT_ARC_RECORD:
			managedPayload.manageVersionBlock(arcRecord, false);
			break;
		default:
			throw new IllegalStateException();
		}
		arcRecord.close();
		if (arcRecord.diagnostics.hasErrors() || arcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
			if (cloner != null) {
				cloner.cloneArcRecord(arcRecord, managedPayload);
			}
		}
	    if (arcRecord.hasPayload() && !arcRecord.hasPseudoEmptyPayload()) {
	    	validate_payload(arcRecord, arcRecord.header.contentType, itemDiagnosis);
	    }
		if ( options.bShowErrors ) {
			//TestResult.showArcErrors( srcFile, arcRecord, System.out );
			if (itemDiagnosis.errors.size() > 0 || itemDiagnosis.warnings.size() > 0) {
				result.rdList.add(itemDiagnosis);
			}
		}
		result.arcErrors += itemDiagnosis.errors.size();
		result.arcWarnings += itemDiagnosis.warnings.size();
		for (int i=0; i<itemDiagnosis.throwables.size(); ++i) {
			TestFileResultItemThrowable itemThrowable = new TestFileResultItemThrowable();
			itemThrowable.startOffset = startOffset;
			itemThrowable.offset = startOffset;
			itemThrowable.t = itemDiagnosis.throwables.get(i);
			result.throwableList.add(itemThrowable);
		}
	}

	@Override
	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException {
		++result.warcRecords;
		//System.out.println(warcRecords + " - " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")");
		TestFileResultItemDiagnosis itemDiagnosis = new TestFileResultItemDiagnosis();
		itemDiagnosis.offset = startOffset;
		itemDiagnosis.type = warcRecord.header.warcTypeStr;
		managedPayload.manageWarcRecord(warcRecord, false);
		warcRecord.close();
		if (warcRecord.diagnostics.hasErrors() || warcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
			if (cloner != null) {
				cloner.cloneWarcRecord(warcRecord, managedPayload);
			}
		}
	    if (warcRecord.hasPayload()) {
	    	validate_payload(warcRecord, warcRecord.header.contentType, itemDiagnosis);
	    }
		if ( options.bShowErrors ) {
			//TestResult.showWarcErrors( srcFile, warcRecord, System.out );
			if (itemDiagnosis.errors.size() > 0 || itemDiagnosis.warnings.size() > 0) {
				result.rdList.add(itemDiagnosis);
			}
		}
		result.warcErrors += itemDiagnosis.errors.size();
		result.warcWarnings += itemDiagnosis.warnings.size();
		for (int i=0; i<itemDiagnosis.throwables.size(); ++i) {
			TestFileResultItemThrowable itemThrowable = new TestFileResultItemThrowable();
			itemThrowable.startOffset = startOffset;
			itemThrowable.offset = startOffset;
			itemThrowable.t = itemDiagnosis.throwables.get(i);
			result.throwableList.add(itemThrowable);
		}
	}

	@Override
	public void apcUpdateConsumed(long consumed) {
		if (callback != null) {
			callback.update(result, consumed);
		}
	}

	@Override
	public void apcRuntimeError(Throwable t, long startOffset, long consumed) {
		TestFileResultItemThrowable itemThrowable = new TestFileResultItemThrowable();
		itemThrowable.startOffset = startOffset;
		itemThrowable.offset = consumed;
		itemThrowable.t = t;
		result.throwableList.add(itemThrowable);
	}

	@Override
	public void apcDone() {
	}

	protected void validate_payload(ArcRecordBase arcRecord, ContentType contentType, TestFileResultItemDiagnosis itemDiagnosis) throws IOException {
    	if (contentType != null) {
    		if ("text".equalsIgnoreCase(contentType.contentType) && "xml".equalsIgnoreCase(contentType.mediaType)) {
        		ValidatorPlugin plugin;
        		for (int i=0; i<options.validatorPlugins.size(); ++i) {
        			plugin = options.validatorPlugins.get(i);
        			plugin.getValidator().validate(managedPayload, itemDiagnosis);
        		}
    		}
    	}

    	/*
        protected static String reFragment = "^(?:[a-zA-Z0-9-._~!$&'()*+,;=:/?@]|%[0-9a-fA-F]{2}|%u[0-9a-fA-F]{4})*";
        protected static Pattern patternFragment = Pattern.compile(reFragment);

        matcher = patternFragment.matcher(fragmentRaw);
        if (!matcher.matches()) {
            throw new URISyntaxException(fragmentRaw, "Invalid URI fragment component");
        }
        */
	}

	protected void validate_payload(WarcRecord warcRecord, ContentType contentType, TestFileResultItemDiagnosis itemDiagnosis) throws IOException {
    	if (contentType != null) {
    		if ("text".equalsIgnoreCase(contentType.contentType) && "xml".equalsIgnoreCase(contentType.mediaType)) {
        		ValidatorPlugin plugin;
        		for (int i=0; i<options.validatorPlugins.size(); ++i) {
        			plugin = options.validatorPlugins.get(i);
        			plugin.getValidator().validate(managedPayload, itemDiagnosis);
        		}
    		}
    	} else {
    		//managedPayloadContentTypeIdentifier.guestimateContentType(managedPayload);
    	}
    }

}
