package org.jwat.tools.tasks.compress;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jwat.gzip.GzipConstants;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipWriter;
import org.jwat.tools.tasks.compress.CompressFile.RecordEntry;
import org.jwat.tools.tasks.compress.ThreadLocalObjectPool.ThreadLocalObjectFactory;

import com.antiaction.common.json.JSONEncoder;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStreamMarshaller;

public class JSONSerializer implements Closeable {

	public static class JSONSerializerFactory implements ThreadLocalObjectFactory<JSONSerializer> {
		@Override
		public JSONSerializer getObject() {
			JSONSerializer jser = null;
			try {
				jser = JSONSerializer.getInstance();
			}
			catch (JSONException e) {
			}
			return jser;
		}
	}

	private static final int GZIP_OUTPUT_BUFFER_SIZE = 16384;

	private JSONEncoding json_encoding;
	private JSONObjectMappings json_objectmappings;
	private JSONEncoder json_encoder;
	private JSONStreamMarshaller json_marshaller;
	private String dstFname;
	private File dstFile;
	private OutputStream out;
	private GzipWriter writer;
	private GzipEntry entry;
	private OutputStream cout;

	protected JSONSerializer() {
	}

	public static JSONSerializer getInstance() throws JSONException {
		JSONSerializer jser = new JSONSerializer();
		jser.json_encoding = JSONEncoding.getJSONEncoding();
		jser.json_objectmappings = new JSONObjectMappings();
		jser.json_objectmappings.register(RecordEntry.class);
		jser.json_encoder = jser.json_encoding.getJSONEncoder(JSONEncoding.E_UTF8);
		return jser;
	}

	public void open(CompressResult result, CompressOptions options) throws JSONException, IOException {
		json_marshaller = json_objectmappings.getStreamMarshaller();
		dstFname = result.dstFile.getName() + ".headers";
		if (options.dstPath == null) {
			dstFile = new File( result.srcFile.getParentFile(), dstFname );
		}
		else {
			dstFile = new File( options.dstPath, dstFname );
		}
		out = new FileOutputStream(dstFile, false);
		writer = new GzipWriter(out, GZIP_OUTPUT_BUFFER_SIZE);
		writer.setCompressionLevel(9);
		entry = new GzipEntry();
		entry.magic = GzipConstants.GZIP_MAGIC;
		entry.cm = GzipConstants.CM_DEFLATE;
		entry.flg = 0;
		entry.mtime = result.srcFile.lastModified() / 1000;
		entry.xfl = 0;
		entry.os = GzipConstants.OS_UNKNOWN;
		writer.writeEntryHeader(entry);
		cout = entry.getOutputStream();
	}

	public void serialize(RecordEntry recordEntry) throws JSONException, IOException {
		json_marshaller.toJSONText(recordEntry, json_encoder, false, cout);
	}

	@Override
	public void close() throws IOException {
		try {
			cout.close();
			cout = null;
			entry.close();
			entry = null;
			writer.close();
			writer = null;
			out.close();
			out = null;
		}
		finally {
			CompressFile.closeIOQuietly(cout);
			CompressFile.closeIOQuietly(entry);
			CompressFile.closeIOQuietly(writer);
			CompressFile.closeIOQuietly(out);
		}
	}

}
