package org.jwat.tools.tasks.headers2cdx;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.gzip.GzipEntry;
import org.jwat.gzip.GzipReader;
import org.jwat.tools.core.IOUtils;
import org.jwat.tools.core.ThreadLocalObjectPool.ThreadLocalObjectFactory;
import org.jwat.tools.tasks.compress.RecordEntry;

import com.antiaction.common.json.JSONDecoder;
import com.antiaction.common.json.JSONDecoderContext;
import com.antiaction.common.json.JSONEncoding;
import com.antiaction.common.json.JSONException;
import com.antiaction.common.json.JSONObjectMappings;
import com.antiaction.common.json.JSONStreamUnmarshaller;

public class JSONDeserializer implements Closeable {

	public static class JSONDeserializerFactory implements ThreadLocalObjectFactory<JSONDeserializer> {
		@Override
		public JSONDeserializer getObject() {
			JSONDeserializer jser = null;
			try {
				jser = JSONDeserializer.getInstance();
			}
			catch (JSONException e) {
			}
			return jser;
		}
	}

	private RandomAccessFile raf = null;
	private RandomAccessFileInputStream rafin = null;
	private ByteCountingPushBackInputStream pbin = null;
	private GzipReader gzipReader = null;
	private GzipEntry gzipEntry = null;
	private InputStream in = null;
	private JSONEncoding json_encoding;
	private JSONObjectMappings json_objectmappings;
	private JSONDecoder json_decoder;
	private JSONStreamUnmarshaller json_unmarshaller;
	private JSONDecoderContext json_decodercontext;

	protected JSONDeserializer() {
	}

	public static JSONDeserializer getInstance() throws JSONException {
		JSONDeserializer jser = new JSONDeserializer();
		jser.json_encoding = JSONEncoding.getJSONEncoding();
		jser.json_decoder = jser.json_encoding.getJSONDecoder(JSONEncoding.E_UTF8);
		jser.json_decodercontext = new JSONDecoderContext( jser.json_decoder, 8192 );
		jser.json_objectmappings = new JSONObjectMappings();
		jser.json_objectmappings.register(RecordEntry.class);
		return jser;
	}

	public void open(File srcFile) throws JSONException, IOException {
		json_unmarshaller = json_objectmappings.getStreamUnmarshaller();
		String srcFname = srcFile.getName();
		RandomAccessFile raf = null;
		RandomAccessFileInputStream rafin;
		ByteCountingPushBackInputStream pbin = null;
		try {
			raf = new RandomAccessFile( srcFile, "r" );
			rafin = new RandomAccessFileInputStream( raf );
			pbin = new ByteCountingPushBackInputStream( new BufferedInputStream( rafin, 8192 ), 32 );
			if (GzipReader.isGzipped(pbin)) {
				/*
				String dstFname;
				if (srcFname.endsWith(".gz")) {
					dstFname = srcFname.substring( 0, srcFname.length() - ".gz".length() );
				}
				else if (srcFname.endsWith(".tgz")) {
					dstFname = srcFname.substring( 0, srcFname.length() - ".tgz".length() ) + ".tar";
				}
				else {
					dstFname = srcFname + ".org";
				}
				File dstFile = new File( srcFile.getParentFile(), dstFname );
				*/
				gzipReader = new GzipReader( pbin );
				gzipEntry = gzipReader.getNextEntry();
				if (gzipEntry != null) {
					in = gzipEntry.getInputStream();
					json_decodercontext.init( in );
				}
				//in.close();
				//gzipEntry.close();
				//gzipReader.close();
			}
			else if ( srcFname.toLowerCase().endsWith( ".gz" ) ) {
				System.out.println( "Invalid extension: " + srcFname );
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public RecordEntry deserialize() throws JSONException, IOException {
		return json_unmarshaller.toObject(in, json_decodercontext, true, RecordEntry.class, null);
	}

	@Override
	public void close() throws IOException {
		IOUtils.closeIOQuietly(in);
		IOUtils.closeIOQuietly(gzipEntry);
		IOUtils.closeIOQuietly(gzipReader);
		IOUtils.closeIOQuietly(pbin);
		IOUtils.closeIOQuietly(rafin);
		IOUtils.closeIOQuietly(raf);
	}

}
