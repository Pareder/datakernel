package io.datakernel.di.impl;

import java.util.concurrent.atomic.AtomicReferenceArray;

public abstract class AbstractRootCompiledBinding<R> implements CompiledBinding<R> {
	private volatile R instance;
	protected final int index;

	protected AbstractRootCompiledBinding(int index) {
		this.index = index;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final R getInstance(AtomicReferenceArray[] scopedInstances, int synchronizedScope) {
		R localInstance = instance;
		if (localInstance != null) return localInstance;
		synchronized (this) {
			if (instance != null) return instance;
			instance = doCreateInstance(scopedInstances, synchronizedScope);
		}
		scopedInstances[0].lazySet(index, instance);
		return instance;
	}

	@Override
	public final R createInstance(AtomicReferenceArray[] scopedInstances, int synchronizedScope) {
		return doCreateInstance(scopedInstances, synchronizedScope);
	}

	protected abstract R doCreateInstance(AtomicReferenceArray[] scopedInstances, int synchronizedScope);

}
