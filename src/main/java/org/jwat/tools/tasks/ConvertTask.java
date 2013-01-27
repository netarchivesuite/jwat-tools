package org.jwat.tools.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HttpHeader;
import org.jwat.common.Payload;
import org.jwat.common.PayloadWithHeaderAbstract;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.tools.JWATTools;
import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.Task;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class ConvertTask extends Task {

	public ConvertTask() {
	}

	@Override
	public void command(CommandLine.Arguments arguments) {
		CommandLine.Argument argument;
		// Thread workers.
		argument = arguments.idMap.get( JWATTools.A_WORKERS );
		if ( argument != null && argument.value != null ) {
			try {
				threads = Integer.parseInt(argument.value);
			} catch (NumberFormatException e) {
			}
		}

		// Files.
		argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;

		threadpool_feeder_lifecycle( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		String srcFname = srcFile.getName();
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin;
		ByteCountingPushBackInputStream pbin = null;
		BufferedOutputStream out = null;
		boolean bCompressed = false;
		int count = 0;
		try {
			raf = new RandomAccessFile( srcFile, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, 8192 ), 16 );
			// This will totally not work on arc.gz files! 
			if (ArcReaderFactory.isArcFile(pbin)) {
				String dstFname = "converted-" + srcFname;
				if (dstFname.toLowerCase().endsWith(".gz")) {
					dstFname = dstFname.substring( 0, dstFname.length() - ".gz".length() );
				}
				if (dstFname.toLowerCase().endsWith(".arc")) {
					dstFname = dstFname.substring( 0, dstFname.length() - ".arc".length() );
				}
				dstFname += ".warc";

				ArcReader reader = ArcReaderFactory.getReader(pbin, 8192);
				ArcRecordBase arcRecord;

				// Should eventually have an override.
				bCompressed = reader.isCompressed();

				System.out.println(srcFname + " -> " + dstFname);

				/*
				 * Check for first record.
				 */

				arcRecord = reader.getNextRecord();
				if (arcRecord != null) {
					out = new BufferedOutputStream(new FileOutputStream(dstFname), 8192);
					WarcWriter writer = WarcWriterFactory.getWriter(out, 8192, bCompressed);
					WarcRecord record;
					Payload payload;
					PayloadWithHeaderAbstract payloadHeaderWrapped;
					HttpHeader httpResponse;
					InputStream in;
					Long contentLength;
					String contentType;

					/*
					 * Conversion warcinfo.
					 */

					UUID warcinfoUuid = UUID.randomUUID();
				    UUID filedescUuid = UUID.randomUUID();

				    GregorianCalendar cal = new GregorianCalendar();
				    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
				    cal.setTimeInMillis(System.currentTimeMillis());

				    record = WarcRecord.createRecord(writer);
					record.header.addHeader(WarcConstants.FN_WARC_TYPE, "warcinfo");
					record.header.addHeader(WarcConstants.FN_WARC_DATE, cal.getTime(), null);
					record.header.addHeader(WarcConstants.FN_WARC_FILENAME, dstFname);
					record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + warcinfoUuid + ">");
					record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, "application/warc-fields");
					record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, "0");
					// Standard says no.
					//record.header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, "<urn:uuid:" + filedescUuid + ">");
					writer.writeHeader(record);
					writer.closeRecord();

					/*
					 * Filedesc metadata.
					 */

					if (arcRecord.recordType == ArcRecordBase.RT_VERSION_BLOCK) {
						record = WarcRecord.createRecord(writer);
						record.header.addHeader(WarcConstants.FN_WARC_TYPE, "metadata");
						record.header.addHeader(WarcConstants.FN_WARC_TARGET_URI, arcRecord.header.urlUri, arcRecord.header.urlStr );
						record.header.addHeader(WarcConstants.FN_WARC_DATE, arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
						record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + filedescUuid + ">");
						record.header.addHeader(WarcConstants.FN_WARC_CONCURRENT_TO, "<urn:uuid:" + warcinfoUuid + ">");
						record.header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, arcRecord.header.inetAddress, arcRecord.header.ipAddressStr);
						record.header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, "<urn:uuid:" + warcinfoUuid + ">");
						// "WARC-Block-Digest"
						// "WARC-Payload-Digest"
						contentLength = 0L;
						contentType = "text/plain";
						ByteArrayOutputStream payloadOut = new ByteArrayOutputStream();
						payloadOut.write(arcRecord.header.headerBytes);
						in = null;
						payload = arcRecord.getPayload();
						if (payload != null) {
							in = payload.getInputStreamComplete();
							byte[] tmpBuf = new byte[8192];
							int read;
							while((read = in.read(tmpBuf)) != -1) {
								payloadOut.write(tmpBuf, 0, read);
							}
							in.close();
						}
						contentLength = new Long(payloadOut.size());
						record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, contentLength, null);
						record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, contentType);
						writer.writeHeader(record);
						writer.writePayload(payloadOut.toByteArray());
						writer.closeRecord();
					}
					else {
						// TODO no version block.
					}
					arcRecord.close();

					/*
					 * Records.
					 */

					UUID recordUuid;
					while ((arcRecord = reader.getNextRecord()) != null) {
						recordUuid = UUID.randomUUID();
						record = WarcRecord.createRecord(writer);
						record.header.addHeader(WarcConstants.FN_WARC_TYPE, "response");
						record.header.addHeader(WarcConstants.FN_WARC_TARGET_URI, arcRecord.header.urlUri, arcRecord.header.urlStr);
						record.header.addHeader(WarcConstants.FN_WARC_DATE, arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
						record.header.addHeader(WarcConstants.FN_WARC_RECORD_ID, "<urn:uuid:" + recordUuid + ">");
						record.header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, arcRecord.header.inetAddress, arcRecord.header.ipAddressStr);
						record.header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, "<urn:uuid:" + warcinfoUuid + ">");
						// "WARC-Block-Digest"
						// "WARC-Payload-Digest"
						contentLength = 0L;
						contentType = null;
						in = null;
						payload = arcRecord.getPayload();
						httpResponse = null;
						if (payload != null) {
							payloadHeaderWrapped = payload.getPayloadHeaderWrapped();
							if (payloadHeaderWrapped instanceof HttpHeader) {
								httpResponse = (HttpHeader)payloadHeaderWrapped;
								if (httpResponse != null && httpResponse.isValid()) {
									if (httpResponse.headerType == HttpHeader.HT_RESPONSE) {
										contentType = "application/http; msgtype=response";
									}
									else if (httpResponse.headerType == HttpHeader.HT_REQUEST) {
										contentType = "application/http; msgtype=request";
									}
									else {
										throw new IllegalStateException("Unknown header type!");
									}
								} else {
									contentType = arcRecord.header.contentTypeStr;
								}
							}
							contentLength = arcRecord.header.archiveLength;
						}
						record.header.addHeader(WarcConstants.FN_CONTENT_LENGTH, contentLength, null);
						if (contentType != null) {
							record.header.addHeader(WarcConstants.FN_CONTENT_TYPE, contentType);
						}
						writer.writeHeader(record);
						if (httpResponse != null && httpResponse.isValid()) {
							in = new ByteArrayInputStream(httpResponse.getHeader());
							writer.streamPayload(in);
						}
						if (payload != null) {
							in = payload.getInputStream();
							writer.streamPayload(in);
						}
						writer.closeRecord();
						if (in != null) {
							in.close();
						}
						arcRecord.close();
					}
					writer.close();
					reader.close();
				} else {
					// TODO no records.
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
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
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

}
