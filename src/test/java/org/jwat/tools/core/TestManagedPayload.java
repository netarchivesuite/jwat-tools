package org.jwat.tools.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

@RunWith(JUnit4.class)
public class TestManagedPayload {

    protected static ClassLoader clsLdr = TestManagedPayload.class.getClassLoader();

    public static final File getTestResourceFile(String fname) {
        URL url = clsLdr.getResource(fname);
        String path = url.getFile();
        path = path.replaceAll("%5b", "[");
        path = path.replaceAll("%5d", "]");
        File file = new File(path);
        return file;
    }

    public byte[] tmpBuf = new byte[8192];

    @Test
	public void test_managedpayload_arc() {
		try {
			ManagedPayloadManager mpm = ManagedPayloadManager.getInstance(1024, 16384);
			File resources = getTestResourceFile("");
			File arcFile = new File(resources, "IAH-20080430204825-00000-blackbook.arc.gz");
			Assert.assertEquals(true, arcFile.exists());
			List<byte[]> arcIndex = indexArcFile(arcFile);
			List<byte[]> arcindex2 = indexArcFilePM(arcFile, mpm);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

	public List<byte[]> indexArcFile(File arcFile) throws IOException {
		List<byte[]> index = new ArrayList<byte[]>();
		RandomAccessFile raf = new RandomAccessFile(arcFile, "r");
		RandomAccessFileInputStream rafis = new RandomAccessFileInputStream(raf);
		ArcReader arcReader = ArcReaderFactory.getReader(rafis, 16384);
		ArcRecordBase arcRecord;
		InputStream in;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read;
		while ((arcRecord = arcReader.getNextRecord()) != null) {
			out.reset();
			in = arcRecord.getPayload().getInputStreamComplete();
			while ((read = in.read(tmpBuf, 0, tmpBuf.length)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			out.close();
			index.add(out.toByteArray());
		}
		return index;
	}

	public List<byte[]> indexArcFilePM(File arcFile, ManagedPayloadManager mpm) throws IOException {
		List<byte[]> index = new ArrayList<byte[]>();
		RandomAccessFile raf = new RandomAccessFile(arcFile, "r");
		RandomAccessFileInputStream rafis = new RandomAccessFileInputStream(raf);
		ArcReader arcReader = ArcReaderFactory.getReader(rafis, 16384);
		ArcRecordBase arcRecord;
		InputStream in;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ManagedPayload managedPayload = mpm.checkout();
		int read;
		while ((arcRecord = arcReader.getNextRecord()) != null) {
			switch (arcRecord.recordType) {
			case ArcRecord.RT_VERSION_BLOCK:
				managedPayload.manageVersionBlock(arcRecord, true);
				break;
			case ArcRecord.RT_ARC_RECORD:
				managedPayload.manageArcRecord(arcRecord, true);
				break;
			}

			out.reset();
			/*
			in = arcRecord.getPayload().getInputStreamComplete();
			while ((read = in.read(tmpBuf, 0, tmpBuf.length)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			*/
			out.close();
			index.add(out.toByteArray());
		}
		managedPayload.close();
		managedPayload.close();
		mpm.checkin(managedPayload);
		return index;
	}

    @Test
	public void test_managedpayload_warc() {
		try {
			ManagedPayloadManager mpm = ManagedPayloadManager.getInstance(1024, 16384);
			File resources = getTestResourceFile("");
			File warcFile = new File(resources, "IAH-20080430204825-00000-blackbook.warc.gz");
			Assert.assertEquals(true, warcFile.exists());
			List<byte[]> arcIndex = indexWarcFile(warcFile);
			List<byte[]> arcindex2 = indexWarcFilePM(warcFile, mpm);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
	}

	public List<byte[]> indexWarcFile(File warcFile) throws IOException {
		List<byte[]> index = new ArrayList<byte[]>();
		RandomAccessFile raf = new RandomAccessFile(warcFile, "r");
		RandomAccessFileInputStream rafis = new RandomAccessFileInputStream(raf);
		WarcReader warcReader = WarcReaderFactory.getReader(rafis, 16384);
		WarcRecord warcRecord;
		InputStream in;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read;
		while ((warcRecord = warcReader.getNextRecord()) != null) {
			out.reset();
			in = warcRecord.getPayload().getInputStreamComplete();
			while ((read = in.read(tmpBuf, 0, tmpBuf.length)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			out.close();
			index.add(out.toByteArray());
		}
		return index;
	}

	public List<byte[]> indexWarcFilePM(File warcFile, ManagedPayloadManager mpm) throws IOException {
		List<byte[]> index = new ArrayList<byte[]>();
		RandomAccessFile raf = new RandomAccessFile(warcFile, "r");
		RandomAccessFileInputStream rafis = new RandomAccessFileInputStream(raf);
		WarcReader warcReader = WarcReaderFactory.getReader(rafis, 16384);
		WarcRecord warcRecord;
		InputStream in;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ManagedPayload managedPayload = mpm.checkout();
		int read;
		while ((warcRecord = warcReader.getNextRecord()) != null) {
			managedPayload.manageWarcRecord(warcRecord, true);
			out.reset();
			/*
			in = arcRecord.getPayload().getInputStreamComplete();
			while ((read = in.read(tmpBuf, 0, tmpBuf.length)) != -1) {
				out.write(tmpBuf, 0, read);
			}
			*/
			out.close();
			index.add(out.toByteArray());
		}
		managedPayload.close();
		managedPayload.close();
		mpm.checkin(managedPayload);
		return index;
	}

}
