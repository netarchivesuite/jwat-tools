package org.jwat.tools.core;

public class ThreadLocalObjectPool<O> {

	public interface ThreadLocalObjectFactory<O> {
		public O getObject();
	}

	private ThreadLocalObjectFactory<O> factory;

	private final ThreadLocal<O> OBJECT_TL = new ThreadLocal<O>() {
		@Override
		public O initialValue() {
			return factory.getObject();
		}
	};

	private ThreadLocalObjectPool(ThreadLocalObjectFactory<O> factory) {
		this.factory = factory;
	}

	public static <O> ThreadLocalObjectPool<O> getPool(ThreadLocalObjectFactory<O> factory) {
		return new ThreadLocalObjectPool<O>(factory);
	}

	public O getThreadLocalObject() {
		return OBJECT_TL.get();
	}

}
