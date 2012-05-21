package org.jwat.tools;

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
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.HttpResponse;
import org.jwat.common.Payload;
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
		int count = 0;
		try {
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( new FileInputStream( srcFile ), 8192 ), 16 );
			if (ArcReaderFactory.isArcFile(pbin)) {
				String dstFname = "converted-" + srcFname;
				if (dstFname.endsWith(".gz")) {
					dstFname = dstFname.substring( 0, dstFname.length() - 3 );
				}
				if (dstFname.endsWith(".arc")) {
					dstFname = dstFname.substring( 0, dstFname.length() - 4 );
				}
				dstFname += ".warc";

				ArcReader reader = ArcReaderFactory.getReader(pbin, 8192);

				System.out.println(srcFname + " -> " + dstFname);

				out = new BufferedOutputStream(new FileOutputStream(dstFname), 8192);
				WarcWriter writer = WarcWriterFactory.getWriter(out, 8192, false);
				WarcRecord record;
				Payload payload;
				HttpResponse httpResponse;
				InputStream in;
				String contentLength;
				String contentType;

				/*
				 * Conversion warcinfo.
				 */

				ArcVersionBlock version = reader.getVersionBlock();

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

				record = WarcRecord.createRecord(writer);
				record.header.addHeader("WARC-Type", "metadata");
				record.header.addHeader("WARC-Target-URI", version.recUrl);
				record.header.addHeader("WARC-Date", version.archiveDate, version.recArchiveDate);
				record.header.addHeader("WARC-Record-ID", "<urn:uuid:" + filedescUuid + ">");
				record.header.addHeader("WARC-Concurrent-To", "<urn:uuid:" + warcinfoUuid + ">");
				// "WARC-Block-Digest"
				// "WARC-Payload-Digest"
				contentLength = "0";
				contentType = null;
				in = null;
				payload = record.getPayload();
				if (version.xml != null && version.xml.length() > 0) {
					contentLength = Long.toString(version.xml.length());
					contentType = version.recContentType;
					in = new ByteArrayInputStream(version.xml.getBytes("ISO8859-1"));
				}
				record.header.addHeader("Content-Length", contentLength);
				if (contentType != null) {
					record.header.addHeader("Content-Type", contentType);
				}
				writer.writeHeader(record);
				if (in != null) {
					writer.streamPayload(in, 0);
				}
				writer.closeRecord();

				/*
				 * Records.
				 */

				ArcRecord arcRecord;
				UUID recordUuid;
				while ((arcRecord = reader.getNextRecord()) != null) {
					recordUuid = UUID.randomUUID();
					record = WarcRecord.createRecord(writer);
					record.header.addHeader("WARC-Type", "metadata");
					record.header.addHeader("WARC-Target-URI", arcRecord.recUrl);
					record.header.addHeader("WARC-Date", arcRecord.archiveDate, arcRecord.recArchiveDate);
					record.header.addHeader("WARC-Record-ID", "<urn:uuid:" + recordUuid + ">");
					// "WARC-Block-Digest"
					// "WARC-Payload-Digest"
					contentLength = "0";
					contentType = null;
					in = null;
					payload = arcRecord.getPayload();
					httpResponse = null;
					if (payload != null) {
						httpResponse = payload.getHttpResponse();
					}
					if (httpResponse != null && httpResponse.isValid()) {
						contentType = "application/http; msgtype=response";
					} else {
						contentType = arcRecord.recContentType;
					}
					if (payload != null) {
						contentLength = Long.toString(arcRecord.recLength);
					}
					record.header.addHeader("Content-Length", contentLength);
					if (contentType != null) {
						record.header.addHeader("Content-Type", contentType);
					}
					writer.writeHeader(record);
					if (httpResponse != null && httpResponse.isValid()) {
						in = new ByteArrayInputStream(httpResponse.getHeader());
						writer.streamPayload(in, 0);
					}
					if (payload != null) {
						in = payload.getInputStream();
						writer.streamPayload(in, 0);
					}
					writer.closeRecord();
				}
				writer.close();
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
