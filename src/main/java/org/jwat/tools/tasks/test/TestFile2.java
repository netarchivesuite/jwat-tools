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

	public void apcFileId(int fileId) {
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

	public void apcArcRecordStart(ArcRecordBase arcRecord, long startOffset, boolean compressed) throws IOException {
		++result.arcRecords;
		//System.out.println(arcRecords + " - " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")");
	    if (arcRecord.hasPayload() && !arcRecord.hasPseudoEmptyPayload()) {
	    	validate_payload(arcRecord, arcRecord.header.contentType, arcRecord.getPayload());
	    }
		arcRecord.close();
		result.arcErrors += arcRecord.diagnostics.getErrors().size();
		result.arcWarnings += arcRecord.diagnostics.getWarnings().size();
		if ( bShowErrors ) {
			//TestResult.showArcErrors( srcFile, arcRecord, System.out );
			if (arcRecord.diagnostics.hasErrors() || arcRecord.diagnostics.hasWarnings()) {
				TestFileResultItemDiagnosis itemDiagnosis = new TestFileResultItemDiagnosis();
				itemDiagnosis.offset = startOffset;
				// TODO arc type string in JWAT.
				itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
				itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
				result.rdList.add(itemDiagnosis);
			}
		}
	}

	public void apcWarcRecordStart(WarcRecord warcRecord, long startOffset, boolean compressed) throws IOException {
		++result.warcRecords;
		//System.out.println(warcRecords + " - " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")");
	    if (warcRecord.hasPayload()) {
	    	validate_payload(warcRecord, warcRecord.header.contentType, warcRecord.getPayload());
	    }
		warcRecord.close();
		result.warcErrors += warcRecord.diagnostics.getErrors().size();
		result.warcWarnings += warcRecord.diagnostics.getWarnings().size();
		if ( bShowErrors ) {
			//TestResult.showWarcErrors( srcFile, warcRecord, System.out );
			if (warcRecord.diagnostics.hasErrors() || warcRecord.diagnostics.hasWarnings()) {
				TestFileResultItemDiagnosis itemDiagnosis = new TestFileResultItemDiagnosis();
				itemDiagnosis.offset = startOffset;
				itemDiagnosis.type = warcRecord.header.warcTypeStr;
				itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
				itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
				result.rdList.add(itemDiagnosis);
			}
		}
	}

	public void apcUpdateConsumed(long consumed) {
		if (callback != null) {
			callback.update(result, consumed);
		}
	}

	public void apcRuntimeError(Throwable t, long startOffset, long consumed) {
		++result.runtimeErrors;
		TestFileResultItemThrowable itemThrowable = new TestFileResultItemThrowable();
		itemThrowable.startOffset = startOffset;
		itemThrowable.offset = consumed;
		itemThrowable.t = t;
		result.throwableList.add(itemThrowable);
	}

	protected void validate_payload(ArcRecordBase arcRecord, ContentType contentType, Payload payload) {
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream());
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

    protected void validate_payload(WarcRecord warcRecord, ContentType contentType, Payload payload) {
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream());
    		}
    	}
    }

}
