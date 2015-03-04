package org.jwat.tools.core;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class ManagedPayloadManager {

	private static final int DEFAULT_COPY_BUFFER_SIZE = 8192;

	private static final int DEFAULT_IN_MEMORY_BUFFER_SIZE = 10*1024*1024;

	public static ManagedPayloadManager getInstance() {
		return getInstance(DEFAULT_COPY_BUFFER_SIZE, DEFAULT_IN_MEMORY_BUFFER_SIZE);
	}

	public static ManagedPayloadManager getInstance(int copyBufferSize, int inMemorybufferSize) {
		ManagedPayloadManager mpm = new ManagedPayloadManager();
		mpm.copyBufferSize = copyBufferSize;
		mpm.inMemorybufferSize = inMemorybufferSize;
		return mpm;
	}

	protected Semaphore queueLock = new Semaphore(1);

	protected ConcurrentLinkedQueue<ManagedPayload> managedPayloadQueue = new ConcurrentLinkedQueue<ManagedPayload>();

	protected int copyBufferSize;

	protected int inMemorybufferSize;

	public ManagedPayload checkout() {
		ManagedPayload managedPayload = null;
		queueLock.acquireUninterruptibly();
		managedPayload = managedPayloadQueue.poll();
		if (managedPayload == null) {
			managedPayload = new ManagedPayload(copyBufferSize, inMemorybufferSize);
		}
		if (!managedPayload.lock.tryAcquire()) {
			throw new IllegalStateException();
		}
		queueLock.release();
		return managedPayload;
	}

	public void checkin(ManagedPayload managedPayload) {
		queueLock.acquireUninterruptibly();
		managedPayload.lock.release();
		managedPayloadQueue.add(managedPayload);
		queueLock.release();
	}

}
