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
		byte[] key = (sector + (destination != null ? destination : "") + doc).getBytes();
		return ringBuffer.get(key);
	}

	@Override
	public void put(@NotNull String sector, @Nullable String destination, @NotNull String doc, ByteBuf pageView) {
		byte[] key = getKey(sector, destination, doc);
		ringBuffer.put(key, pageView.array());
	}

	private byte[] getKey(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return  (sector + (destination != null ? destination : "") + doc).getBytes();
	}
}
