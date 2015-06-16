package org.jwat.tools.tasks.arc2warc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.archive.ManagedPayload;
import org.jwat.common.Base32;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HttpHeader;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class Arc2Warc {

	public File srcFile;

	protected RepairPayload repairPayload;

	public List<Throwable> exceptionList = new LinkedList<Throwable>();

	/**
	 * 
	 * @param srcFile a valid arc(.gz) file
	 */
	public void arc2warc(File srcFile, File destDir, String prefix, boolean bOverwrite) {
		repairPayload = RepairPayload.getRepairPayload();
		try {
			String srcFname = srcFile.getName();
			RandomAccessFile raf = null;
			RandomAccessFileInputStream rafin;
			ByteCountingPushBackInputStream file_in = null;

			ArcReader reader = null;
			boolean bSrcCompressed;
			ArcRecordBase arcRecord;

			BufferedOutputStream file_out = null;
			WarcWriter writer = null;
			boolean bDestCompressed;
			WarcRecord record;

			Long contentLength;
			String contentType;

			ManagedPayload managedPayload = null;
			InputStream payloadStream;
			HttpHeader httpHeader;
	        WarcDigest warcBlockDigest;
	        WarcDigest warcPayloadDigest;

			try {
				/*
				 * Source.
				 */

				raf = new RandomAccessFile( srcFile, "r" );
				rafin = new RandomAccessFileInputStream( raf );
				file_in = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, 8192 ), 32 );

				reader = ArcReaderFactory.getReader(file_in, 8192);
				bSrcCompressed = reader.isCompressed();

				/*
				 * Destination.
				 */

				// TODO select converted compression on/off/same.
				bDestCompressed = false;

				String dstFname = prefix + srcFname;
				if (dstFname.toLowerCase().endsWith(".gz")) {
					dstFname = dstFname.substring( 0, dstFname.length() - ".gz".length() );
				}
				if (dstFname.toLowerCase().endsWith(".arc")) {
					dstFname = dstFname.substring( 0, dstFname.length() - ".arc".length() );
				}
				dstFname += ".warc";
				if (bDestCompressed) {
					dstFname += ".gz";				
				}
				String tmpFname = dstFname + ".open";

				File tmpDstFile = new File(destDir, tmpFname);
				File dstFile = new File(destDir, dstFname);

				if (dstFile.exists()) {
					if (!dstFile.isFile()) {
						throw new IOException("Destination file is a directory: '" + dstFile.getPath() + "'");
					}
					if (bOverwrite && !dstFile.delete()) {
						throw new IOException("Could not delete file: '" + dstFile.getPath() + "'");
					}
				}

				if (!dstFile.exists()) {
					if (tmpDstFile.exists()) {
						if (!tmpDstFile.isFile()) {
							throw new IOException("Temporary destination file is a directory: '" + tmpDstFile.getPath() + "'");
						}
						if (!tmpDstFile.delete()) {
							throw new IOException("Could not delete file: '" + tmpDstFile.getPath() + "'");
						}
					}

					file_out = new BufferedOutputStream(new FileOutputStream(tmpDstFile), 8192);
					writer = WarcWriterFactory.getWriter(file_out, 8192, bDestCompressed);

					// debug
					//System.out.println(srcFname + " -> " + dstFname);

					managedPayload = ManagedPayload.checkout();

					/*
					 * Loop record(s).
					 */

					UUID warcinfoUuid = null;
				    UUID filedescUuid = null;
					UUID recordUuid = null;

					int recordCount = 0;

					while ((arcRecord = reader.getNextRecord()) != null) {
						/*
						 *  Generate filedesc uuid if the arc record is a version block record.
						 */
						if (arcRecord.recordType == ArcRecordBase.RT_VERSION_BLOCK) {
						    filedescUuid = UUID.randomUUID();
						}
						/*
						 * Is the first record a version block record?
						 */
						if (recordCount == 0 && arcRecord.recordType != ArcRecordBase.RT_VERSION_BLOCK) {
							// TODO Warning, missing filedesc as first record in ARC file.
						}
						/*
						 *  Write a warcinfo record if is the first record or if it is a version block record.
						 */
						if (recordCount == 0 || arcRecord.recordType == ArcRecordBase.RT_VERSION_BLOCK) {
							GregorianCalendar cal = new GregorianCalendar();
						    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
						    cal.setTimeInMillis(System.currentTimeMillis());
							warcinfoUuid = UUID.randomUUID();
						    record = WarcRecord.createRecord(writer);
							record.header.addHeader(WarcConstants.FN_WARC_TYPE, WarcConstants.RT_WARCINFO);
							record.header.addHeader(WarcConstants.FN_WARC_DATE, cal.getTime(), null);
							record.header.addHeader(WarcConstants.FN_WARC_FILENAME, dstFname);
							record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + warcinfoUuid + ">");
							record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, "application/warc-fields");
							record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, "0");
							// Standard says no.
							//record.header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, "<urn:uuid:" + filedescUuid + ">");
							writer.writeHeader(record);
							writer.closeRecord();
							++recordCount;
						}
						/*
						 * Write filedesc metadata is the record is a version block record.
						 */
						if (arcRecord.recordType == ArcRecordBase.RT_VERSION_BLOCK) {
							managedPayload.manageVersionBlock(arcRecord, true);

							contentType = "text/plain";
							contentLength = managedPayload.payloadLength;
							warcBlockDigest = WarcDigest.createWarcDigest("SHA1", managedPayload.blockDigestBytes, "base32", Base32.encodeArray(managedPayload.blockDigestBytes));

							record = WarcRecord.createRecord(writer);
							record.header.addHeader(WarcConstants.FN_WARC_TYPE, WarcConstants.RT_METADATA);
							record.header.addHeader(WarcConstants.FN_WARC_TARGET_URI, arcRecord.header.urlUri, arcRecord.header.urlStr );
							record.header.addHeader(WarcConstants.FN_WARC_DATE, arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
							record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + filedescUuid + ">");
							record.header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, "<urn:uuid:" + warcinfoUuid + ">");
							record.header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, arcRecord.header.inetAddress, arcRecord.header.ipAddressStr);
							record.header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, "<urn:uuid:" + warcinfoUuid + ">");
							record.header.addHeader(WarcConstants.FN_WARC_BLOCK_DIGEST, warcBlockDigest, null);
							record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, contentLength, null);
							record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, contentType);
							writer.writeHeader(record);
							payloadStream = managedPayload.getPayloadStream();
							if (payloadStream != null) {
								writer.streamPayload(payloadStream);
								payloadStream.close();
								payloadStream = null;
							}
							writer.closeRecord();
							arcRecord.close();
							++recordCount;
						}
						else {
							/*
							 * Response.
							 */
							/*
							if (recordCount == 901) {
								System.out.println(recordCount);
							}
							*/
							managedPayload.manageArcRecord(arcRecord, true);

							httpHeader = managedPayload.httpHeader;
							if (httpHeader == null || !httpHeader.isValid()) {
								//savePayload(managedPayload);

								// Optional
								/*
								int number = getNextPayloadErrorNumber();
								savePayloadErrorArc(arcRecord, managedPayload, number);
								*/

								managedPayload = repairPayload.repairPayload(managedPayload, arcRecord.header.contentTypeStr, arcRecord.header.archiveDate);
							}

							httpHeader = managedPayload.httpHeader;
							if (httpHeader != null && httpHeader.isValid()) {
								if (httpHeader.headerType == HttpHeader.HT_RESPONSE) {
									contentType = "application/http; msgtype=response";
								}
								else if (httpHeader.headerType == HttpHeader.HT_REQUEST) {
									contentType = "application/http; msgtype=request";
								}
								else {
									throw new IllegalStateException("Unknown header type!");
								}
							} else {
								contentType = arcRecord.header.contentTypeStr;
							}

							warcBlockDigest = WarcDigest.createWarcDigest("SHA1", managedPayload.blockDigestBytes, "base32", Base32.encodeArray(managedPayload.blockDigestBytes));
							warcPayloadDigest = WarcDigest.createWarcDigest("SHA1", managedPayload.payloadDigestBytes, "base32", Base32.encodeArray(managedPayload.payloadDigestBytes));

							recordUuid = UUID.randomUUID();
							record = WarcRecord.createRecord(writer);
							record.header.addHeader(WarcConstants.FN_WARC_TYPE, WarcConstants.RT_RESPONSE);
							record.header.addHeader(WarcConstants.FN_WARC_TARGET_URI, arcRecord.header.urlUri, arcRecord.header.urlStr);
							record.header.addHeader(WarcConstants.FN_WARC_DATE, arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
							record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + recordUuid + ">");
							record.header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, arcRecord.header.inetAddress, arcRecord.header.ipAddressStr);
							record.header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, "<urn:uuid:" + warcinfoUuid + ">");
							contentLength = managedPayload.httpHeaderLength + managedPayload.payloadLength;
							if (contentLength > 0) {
								record.header.addHeader(WarcConstants.FN_WARC_BLOCK_DIGEST, warcBlockDigest, null);
								if (managedPayload.httpHeaderLength > 0 && managedPayload.payloadLength > 0) {
									record.header.addHeader(WarcConstants.FN_WARC_PAYLOAD_DIGEST, warcPayloadDigest, null);
								}
							}
							record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, contentLength, null);
							if (contentType != null) {
								record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, contentType);
							}
							writer.writeHeader(record);
							InputStream httpHeaderStream = managedPayload.getHttpHeaderStream();
							if (httpHeaderStream != null) {
								writer.streamPayload(httpHeaderStream);
								httpHeaderStream.close();
								httpHeaderStream = null;
							}
							payloadStream = managedPayload.getPayloadStream();
							if (payloadStream != null) {
								writer.streamPayload(payloadStream);
								payloadStream.close();
								payloadStream = null;
							}
							writer.closeRecord();
							arcRecord.close();
							++recordCount;							
						}
					}
					if (!tmpDstFile.renameTo(dstFile)) {
						throw new IOException("Could not rename '" + tmpDstFile.getPath() + "' to '" + dstFile.getPath() + "'");
					}
				}
			}
			catch (FileNotFoundException e) {
				exceptionList.add(e);
			}
			catch (IOException e) {
				exceptionList.add(e);
			}
			finally {
				if (managedPayload != null) {
					managedPayload.checkin();
				}
				if (writer != null) {
					try {
						writer.close();
						writer = null;
					}
					catch (IOException e) {
						exceptionList.add(e);
					}
				}
				if (file_out != null) {
					try {
						file_out.close();
						file_out = null;
					}
					catch (IOException e) {
						exceptionList.add(e);
					}
				}
				if (reader != null) {
					// TODO arcreader.close throw exception?
					reader.close();
					reader = null;
					/*
					try {
					}
					catch (IOException e) {
					}
					*/
				}
				if (file_in != null) {
					try {
						file_in.close();
						file_in = null;
					}
					catch (IOException e) {
						exceptionList.add(e);
					}
				}
				if (raf != null) {
					try {
						raf.close();
						raf = null;
					}
					catch (IOException e) {
						exceptionList.add(e);
					}
				}
			}
		} catch (Throwable t) {
			exceptionList.add(t);
		}
		for (int i=0; i<exceptionList.size(); ++i) {
			System.out.println(srcFile.getPath());
			exceptionList.get(i).printStackTrace();
		}
	}

	public static volatile int payload_error_number = 1;

	public synchronized int getNextPayloadErrorNumber() {
		return payload_error_number++;
	}

	public static void savePayloadErrorArc(ArcRecordBase record, ManagedPayload managedPayload, int number) throws IOException {
		InputStream in;
		byte[] copyBuffer = new byte[8192];
		int read;

		RandomAccessFile raf;
		raf = new RandomAccessFile("erroneous-httpheader-" + number + ".arc", "rw");
		raf.seek(0);
		raf.setLength(0);
		raf.write(record.header.headerBytes);
		in = managedPayload.getHttpHeaderStream();
		if (in != null) {
			while ((read = in.read(copyBuffer)) != -1) {
				raf.write(copyBuffer, 0, read);
			}
		}
		in = managedPayload.getPayloadStream();
		if (in != null) {
			while ((read = in.read(copyBuffer)) != -1) {
				raf.write(copyBuffer, 0, read);
			}
		}
		in.close();
		raf.close();
	}

	/*
	public static int payloadNumber = 1;

	public static byte[] payloadCopyBuf = new byte[8192];

	public static synchronized void savePayload(ManagedPayload managedPayload) {
		try {
			InputStream in = managedPayload.getPayloadStream();
			RandomAccessFile raf = new RandomAccessFile("missing-httpheader-" + payloadNumber, "rw");
			int read;
			while ((read = in.read(payloadCopyBuf)) != -1) {
				raf.write(payloadCopyBuf, 0, read);
			}
			raf.close();
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		++payloadNumber;
	}
	*/

}
