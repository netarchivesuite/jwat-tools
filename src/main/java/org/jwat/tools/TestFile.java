package org.jwat.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

public class TestFile {

	public static boolean checkfile(File file) {
		boolean bValidate = false;
		FileInputStream in = null;
		ByteCountingPushBackInputStream pbin = null;
		try {
			byte[] magicBytes = new byte[16];
			int magicLength = 0;
			in = new FileInputStream(file);
			pbin = new ByteCountingPushBackInputStream(in, 16);
			magicLength = pbin.readFully(magicBytes);
			if (magicLength == 16) {
				if (GzipReader.isGzipped(pbin)) {
					bValidate = true;
				} else if (ArcReaderFactory.isArcFile(pbin)) {
					bValidate = true;
				} else if (WarcReaderFactory.isWarcFile(pbin)) {
					bValidate = true;
				} else {
					String fname = file.getName().toLowerCase();
					if (fname.endsWith(".gz")) {
						bValidate = true;
					} else if (fname.endsWith(".arc")) {
						bValidate = true;
					} else if (fname.endsWith(".warc")) {
						bValidate = true;
					}
				}
			}
			pbin.close();
			pbin = null;
			in.close();
			in = null;
		} catch (FileNotFoundException e) {
			System.out.println("Error reading: " + file.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (pbin != null) {
				try {
					pbin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bValidate;
	}

	public static TestFileResult processFile(File file, boolean bShowErrors, TestFileUpdateCallback callback) {
		//System.out.println(">" + file.getPath());
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
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( file ), 8192 ), 16 );
			if ( GzipReader.isGzipped( pbin ) ) {
				gzipReader = new GzipReader( pbin );
				ByteCountingPushBackInputStream in;
				byte[] buffer = new byte[ 8192 ];
				while ( (gzipEntry = gzipReader.getNextEntry()) != null ) {
					in = new ByteCountingPushBackInputStream( new BufferedInputStream( gzipEntry.getInputStream(), 8192 ), 16 );
					++result.gzipEntries;
					//System.out.println(gzipEntries + " - " + gzipEntry.getStartOffset() + " (0x" + (Long.toHexString(gzipEntry.getStartOffset())) + ")");
					if ( result.gzipEntries == 1 ) {
						if ( ArcReaderFactory.isArcFile( in ) ) {
							arcReader = ArcReaderFactory.getReaderUncompressed();
							arcReader.setBlockDigestEnabled( true );
							arcReader.setPayloadDigestEnabled( true );
							++result.arcGzFiles;
						}
						else if ( WarcReaderFactory.isWarcFile( in ) ) {
							warcReader = WarcReaderFactory.getReaderUncompressed();
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
				arcReader.setBlockDigestEnabled( true );
				arcReader.setPayloadDigestEnabled( true );
				while ( (arcRecord = arcReader.getNextRecord()) != null ) {
				    ++result.arcRecords;
					//System.out.println(arcRecords + " - " + arcRecord.getStartOffset() + " (0x" + (Long.toHexString(arcRecord.getStartOffset())) + ")");
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
				warcReader.setBlockDigestEnabled( true );
				warcReader.setPayloadDigestEnabled( true );
				while ( (warcRecord = warcReader.getNextRecord()) != null ) {
					++result.warcRecords;
					//System.out.println(warcRecords + " - " + warcRecord.getStartOffset() + " (0x" + (Long.toHexString(warcRecord.getStartOffset())) + ")");
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
			++result.runtimeErrors;
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
				length = gzipEntry.comp_isize;
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
		}
		result.bGzipReader = gzipReader != null;
		result.bArcReader = arcReader != null;
		result.bWarcReader = warcReader != null;
		if (gzipReader != null) {
			//System.out.println( "    GZip.isValid: " + gzipReader.isCompliant() );
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

}
