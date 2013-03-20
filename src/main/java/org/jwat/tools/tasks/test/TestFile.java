package org.jwat.tools.tasks.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.Payload;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.common.UriProfile;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.tools.core.ValidatorPlugin;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class TestFile {

	public List<ValidatorPlugin> validatorPlugins = new LinkedList<ValidatorPlugin>();

	public UriProfile uriProfile = UriProfile.RFC3986;

	public TestFileResult processFile(File file, boolean bShowErrors, TestFileUpdateCallback callback) {
		//System.out.println(">" + file.getPath());
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin;
		ByteCountingPushBackInputStream pbin = null;
		GzipReader gzipReader = null;
		ArcReader arcReader = null;
		WarcReader warcReader = null;
		GzipEntry gzipEntry = null;
		ArcRecordBase arcRecord = null;
		WarcRecord warcRecord = null;
		TestFileResult result = new TestFileResult();
		TestFileResultItemDiagnosis itemDiagnosis;
		TestFileResultItemThrowable itemThrowable;
		result.file = file.getPath();
		try {
			raf = new RandomAccessFile( file, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, 8192 ), 32 );
			if ( GzipReader.isGzipped( pbin ) ) {
				gzipReader = new GzipReader( pbin );
				ByteCountingPushBackInputStream in;
				byte[] buffer = new byte[ 8192 ];
				while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
					in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzipEntry.getInputStream(), 8192 ), 32 );
					++result.gzipEntries;
					//System.out.println(gzipEntries + " - " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")");
					if ( result.gzipEntries == 1 ) {
						if ( ArcReaderFactory.isArcFile( in ) ) {
							arcReader = ArcReaderFactory.getReaderUncompressed();
							arcReader.setUriProfile(uriProfile);
							arcReader.setBlockDigestEnabled( true );
							arcReader.setPayloadDigestEnabled( true );
							++result.arcGzFiles;
						}
						else if ( WarcReaderFactory.isWarcFile( in ) ) {
							warcReader = WarcReaderFactory.getReaderUncompressed();
							warcReader.setWarcTargetUriProfile(uriProfile);
							warcReader.setBlockDigestEnabled( true );
							warcReader.setPayloadDigestEnabled( true );
							++result.warcGzFiles;
						}
						else {
							++result.gzFiles;
						}
					}
					if ( arcReader != null ) {
						while ( (arcRecord = arcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() )) != null ) {
						    ++result.arcRecords;
						    if (arcRecord.hasPayload() && !arcRecord.hasPseudoEmptyPayload()) {
						    	validate_payload(arcRecord, arcRecord.header.contentType, arcRecord.getPayload());
						    }
						    arcRecord.close();
							result.arcErrors += arcRecord.diagnostics.getErrors().size();
							result.arcWarnings += arcRecord.diagnostics.getWarnings().size();
							if ( bShowErrors ) {
								//TestResult.showArcErrors( srcFile, arcRecord, System.out );
								if (arcRecord.diagnostics.hasErrors() || arcRecord.diagnostics.hasWarnings()) {
									itemDiagnosis = new TestFileResultItemDiagnosis();
									itemDiagnosis.offset = gzipReader.getStartOffset();
									// TODO arc type string in JWAT.
									itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
									itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
									result.rdList.add(itemDiagnosis);
								}
							}
						}
					}
					else if ( warcReader != null ) {
						while ( (warcRecord = warcReader.getNextRecordFrom( in, gzipEntry.getStartOffset() ) ) != null ) {
							++result.warcRecords;
						    if (warcRecord.hasPayload()) {
						    	validate_payload(warcRecord, warcRecord.header.contentType, warcRecord.getPayload());
						    }
							warcRecord.close();
							result.warcErrors += warcRecord.diagnostics.getErrors().size();
							result.warcWarnings += warcRecord.diagnostics.getWarnings().size();
							if ( bShowErrors ) {
								//TestResult.showWarcErrors( srcFile, warcRecord, System.out );
								if (warcRecord.diagnostics.hasErrors() || warcRecord.diagnostics.hasWarnings()) {
									itemDiagnosis = new TestFileResultItemDiagnosis();
									itemDiagnosis.offset = gzipReader.getStartOffset();
									itemDiagnosis.type = warcRecord.header.warcTypeStr;
									itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
									itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
									result.rdList.add(itemDiagnosis);
								}
							}
						}
					}
					else {
						while ( in.read(buffer) != -1 ) {
						}
					}
					in.close();
					gzipEntry.close();
					result.gzipErrors = gzipEntry.diagnostics.getErrors().size();
					result.gzipWarnings = gzipEntry.diagnostics.getWarnings().size();
					if ( bShowErrors ) {
						//TestResult.showGzipErrors(srcFile, gzipEntry, System.out);
						if (gzipEntry.diagnostics.hasErrors() || gzipEntry.diagnostics.hasWarnings()) {
							itemDiagnosis = new TestFileResultItemDiagnosis();
							itemDiagnosis.offset = gzipReader.getStartOffset();
							itemDiagnosis.errors = gzipEntry.diagnostics.getErrors();
							itemDiagnosis.warnings = gzipEntry.diagnostics.getWarnings();
							result.rdList.add(itemDiagnosis);
						}
					}
					if (callback != null) {
						callback.update(result, pbin.getConsumed());
					}
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
				arcReader.setUriProfile(uriProfile);
				arcReader.setBlockDigestEnabled( true );
				arcReader.setPayloadDigestEnabled( true );
				while ( (arcRecord = arcReader.getNextRecord()) != null ) {
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
							itemDiagnosis = new TestFileResultItemDiagnosis();
							itemDiagnosis.offset = arcReader.getStartOffset();
							itemDiagnosis.errors = arcRecord.diagnostics.getErrors();
							itemDiagnosis.warnings = arcRecord.diagnostics.getWarnings();
							result.rdList.add(itemDiagnosis);
						}
					}
					if (callback != null) {
						callback.update(result, pbin.getConsumed());
					}
				}
				arcReader.close();
				++result.arcFiles;
			}
			else if ( WarcReaderFactory.isWarcFile( pbin ) ) {
				warcReader = WarcReaderFactory.getReader( pbin );
				warcReader.setWarcTargetUriProfile(uriProfile);
				warcReader.setBlockDigestEnabled( true );
				warcReader.setPayloadDigestEnabled( true );
				while ( (warcRecord = warcReader.getNextRecord()) != null ) {
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
							itemDiagnosis = new TestFileResultItemDiagnosis();
							itemDiagnosis.offset = warcReader.getStartOffset();
							itemDiagnosis.type = warcRecord.header.warcTypeStr;
							itemDiagnosis.errors = warcRecord.diagnostics.getErrors();
							itemDiagnosis.warnings = warcRecord.diagnostics.getWarnings();
							result.rdList.add(itemDiagnosis);
						}
					}
					if (callback != null) {
						callback.update(result, pbin.getConsumed());
					}
				}
				warcReader.close();
				++result.warcFiles;
			}
			else {
				++result.skipped;
			}
		}
		catch (Throwable t) {
			itemThrowable = new TestFileResultItemThrowable();
			long startOffset = -1;
			Long length = null;
			if (arcRecord != null) {
				startOffset = arcRecord.getStartOffset();
				length = arcRecord.header.archiveLength;
			}
			if (warcRecord != null) {
				startOffset = warcRecord.getStartOffset();
				length = warcRecord.header.contentLength;
			}
			if (gzipEntry != null) {
				startOffset = gzipEntry.getStartOffset();
				// TODO correct entry size including header+trailer.
				length = gzipEntry.compressed_size;
			}
			if (length != null) {
				startOffset += length;
			}
			itemThrowable.startOffset = startOffset;
			itemThrowable.offset = pbin.getConsumed();
			itemThrowable.t = t;
			result.throwableList.add(itemThrowable);
		}
		finally {
			if (pbin != null) {
				try {
					pbin.close();
				}
				catch (IOException e) {
				}
			}
			if (raf != null) {
				try {
					raf.close();
				}
				catch (IOException e) {
				}
			}
		}
		result.bGzipReader = gzipReader != null;
		result.bArcReader = arcReader != null;
		result.bWarcReader = warcReader != null;
		if (gzipReader != null) {
			result.bGzipIsComppliant = gzipReader.isCompliant();
		}
		if (arcReader != null) {
			result.bArcIsCompliant = arcReader.isCompliant();
		}
		if (warcReader != null) {
			result.bWarcIsCompliant = warcReader.isCompliant();
		}
		//System.out.println("<" + file.getPath());
		if (callback != null) {
			callback.finalUpdate(result, pbin.getConsumed());
		}
		return result;
	}

	protected void validate_payload(ArcRecordBase arcRecord, ContentType contentType, Payload payload) {
		/*
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream(), null);
    		}
    	}
    	*/

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
    	/*
    	if (contentType != null
    			&& "text".equalsIgnoreCase(contentType.contentType)
    			&& "xml".equalsIgnoreCase(contentType.mediaType)) {
    		ValidatorPlugin plugin;
    		for (int i=0; i<validatorPlugins.size(); ++i) {
    			plugin = validatorPlugins.get(i);
    			plugin.getValidator().validate(payload.getInputStream(), null);
    		}
    	}
    	*/
    }

}
