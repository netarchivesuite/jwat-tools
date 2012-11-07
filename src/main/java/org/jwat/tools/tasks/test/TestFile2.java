package org.jwat.tools.tasks.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ContentType;
import org.jwat.common.Payload;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.tools.core.ArchiveParser;
import org.jwat.tools.core.ArchiveParserCallback;
import org.jwat.tools.core.FileIdent;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.warc.WarcRecord;

public class TestFile2 implements ArchiveParserCallback {

	public boolean bShowErrors;

	public UriProfile uriProfile;

    public int recordHeaderMaxSize = 8192;

    public int payloadHeaderMaxSize = 32768;

    public List<ValidatorPlugin> validatorPlugins;

	public TestFileUpdateCallback callback;

	public TestFileResult result;

	public TestFileResult processFile(File file) {
		result = new TestFileResult();
		result.file = file.getPath();

		ArchiveParser archiveParser = new ArchiveParser();
		archiveParser.uriProfile = uriProfile;
		archiveParser.bBlockDigestEnabled = true;
		archiveParser.bPayloadDigestEnabled = true;
	    archiveParser.recordHeaderMaxSize = recordHeaderMaxSize;
	    archiveParser.payloadHeaderMaxSize = payloadHeaderMaxSize;

		long consumed = archiveParser.parse(file, this);

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
		if ( bShowErrors ) {
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
	    if (arcRecord.hasPayload() && !arcRecord.hasPseudoEmptyPayload()) {
	    	validate_payload(arcRecord, arcRecord.header.contentType, arcRecord.getPayload(), itemDiagnosis);
	    }
		arcRecord.close();
		if (arcRecord.diagnostics.hasErrors() || arcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
		}
		if ( bShowErrors ) {
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
	    if (warcRecord.hasPayload()) {
	    	validate_payload(warcRecord, warcRecord.header.contentType, warcRecord.getPayload(), itemDiagnosis);
	    }
		warcRecord.close();
		if (warcRecord.diagnostics.hasErrors() || warcRecord.diagnostics.hasWarnings()) {
			itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
			itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
		}
		if ( bShowErrors ) {
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

	protected void validate_payload(ArcRecordBase arcRecord, ContentType contentType, Payload payload, TestFileResultItemDiagnosis itemDiagnosis) {
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream(), itemDiagnosis);
    			//plugin.getValidator().validate(payload.getInputStream(), itemDiagnosis);
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

    protected void validate_payload(WarcRecord warcRecord, ContentType contentType, Payload payload, TestFileResultItemDiagnosis itemDiagnosis) {
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream(), itemDiagnosis);
    			//plugin.getValidator().validate(payload.getInputStream(), itemDiagnosis);
    		}
    	}
    }

}
