package io.datakernel.render;

import io.datakernel.bytebuf.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PageCache {
	@Nullable ByteBuf get(@NotNull String url);
	void put(@NotNull String url, ByteBuf pageView);
}
