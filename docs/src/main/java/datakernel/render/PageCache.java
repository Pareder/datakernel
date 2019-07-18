package datakernel.render;

import io.datakernel.bytebuf.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PageCache {
	@Nullable ByteBuf get(@NotNull String sector, @Nullable String destination, @NotNull String doc);
	void put(@NotNull String sector, @Nullable String destination, @NotNull String doc, ByteBuf pageView);
}
