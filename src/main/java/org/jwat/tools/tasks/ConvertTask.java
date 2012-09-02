package org.jwat.tools.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.jwat.tools.CommandLine;
import org.jwat.tools.JWATTools;
import org.jwat.tools.Task;
import org.jwat.tools.CommandLine.Argument;
import org.jwat.tools.CommandLine.Arguments;
import org.jwat.warc.WarcRecord;
import org.jwat.warc.WarcWriter;
import org.jwat.warc.WarcWriterFactory;

public class ConvertTask extends Task {

	public ConvertTask(CommandLine.Arguments arguments) {
		CommandLine.Argument argument = arguments.idMap.get( JWATTools.A_FILES );
		List<String> filesList = argument.values;
		taskFileListFeeder( filesList, this );
	}

	@Override
	public void process(File srcFile) {
		String srcFname = srcFile.getName();
		ByteCountingPushBackInputStream pbin = null;
		RandomAccessFile rafIn = null;
		BufferedOutputStream out = null;
		boolean bCompressed = false;
		int count = 0;
		try {
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( srcFile ), 8192 ), 16 );
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
					record.header.addHeader("WARC-Type", "warcinfo");
					record.header.addHeader("WARC-Date", cal.getTime(), null);
					record.header.addHeader("WARC-Filename", dstFname);
					record.header.addHeader("WARC-Record-ID", "<urn:uuid:" + warcinfoUuid + ">");
					record.header.addHeader("Content-Type", "application/warc-fields");
					record.header.addHeader("Content-Length", "0");
					// Standard says no.
					//record.header.addHeader("WARC-Concurrent-To", "<urn:uuid:" + filedescUuid + ">");
					writer.writeHeader(record);
					writer.closeRecord();

					/*
					 * Filedesc metadata.
					 */

					if (arcRecord.recordType == ArcRecordBase.RT_VERSION_BLOCK) {
						record = WarcRecord.createRecord(writer);
						record.header.addHeader("WARC-Type", "metadata");
						record.header.addHeader("WARC-Target-URI", arcRecord.header.urlUri, arcRecord.header.urlStr );
						record.header.addHeader("WARC-Date", arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
						record.header.addHeader("WARC-Record-ID", "<urn:uuid:" + filedescUuid + ">");
						record.header.addHeader("WARC-Concurrent-To", "<urn:uuid:" + warcinfoUuid + ">");
						// "WARC-Block-Digest"
						// "WARC-Payload-Digest"
						contentLength = 0L;
						contentType = "text/plain";
						in = null;
						payload = arcRecord.getPayload();
						if (payload != null) {
							// TODO Add record line when available in JWAT.
							in = payload.getInputStreamComplete();
							contentLength = payload.getTotalLength();
						}
						record.header.addHeader("Content-Length", contentLength, null);
						if (contentType != null) {
							record.header.addHeader("Content-Type", contentType);
						}
						writer.writeHeader(record);
						if (in != null) {
							writer.streamPayload(in);
						}
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
						record.header.addHeader("WARC-Type", "response");
						record.header.addHeader("WARC-Target-URI", arcRecord.header.urlUri, arcRecord.header.urlStr);
						record.header.addHeader("WARC-Date", arcRecord.header.archiveDate, arcRecord.header.archiveDateStr);
						record.header.addHeader("WARC-Record-ID", "<urn:uuid:" + recordUuid + ">");
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
						record.header.addHeader("Content-Length", contentLength, null);
						if (contentType != null) {
							record.header.addHeader("Content-Type", contentType);
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
						arcRecord.close();
					}
					writer.close();
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
			if (rafIn != null) {
				try {
					rafIn.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

}
