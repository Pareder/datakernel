package datakernel.render;

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
	public ByteBuf get(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return ringBuffer.get(getKey(sector, destination, doc));
	}

	@Override
	public void put(@NotNull String sector, @Nullable String destination, @NotNull String doc, ByteBuf pageView) {
		ringBuffer.put(getKey(sector, destination, doc), pageView.getArray());
	}

	private byte[] getKey(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return (sector + (destination != null ? destination : "") + doc).getBytes();
	}
}
