package io.datakernel.render;

import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import org.jetbrains.annotations.NotNull;

public interface PageRenderer {
	Promise<ByteBuf> render(@NotNull String templatePath, @NotNull String url);
}
