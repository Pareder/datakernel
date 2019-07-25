package io.datakernel.render;

import io.datakernel.bytebuf.ByteBuf;
import io.datakernel.memcache.server.RingBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PageCacheImpl implements PageCache {
	private final RingBuffer ringBuffer;

	public PageCacheImpl(int amountBuffer, int bufferCapacity) {
		this.ringBuffer = RingBuffer.create(amountBuffer, bufferCapacity);
	}

	@Override
	public ByteBuf get(@NotNull String url) {
		return ringBuffer.get(url.getBytes());
	}

	@Override
	public void put(@NotNull String url, ByteBuf pageView) {
		ringBuffer.put(url.getBytes(), pageView.getArray());
	}
}
