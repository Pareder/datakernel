package datakernel.render;

import com.github.mustachejava.Mustache;
import io.datakernel.async.Promise;
import io.datakernel.bytebuf.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PageRenderer {
	Promise<ByteBuf> render(Mustache page, @NotNull String sector, @Nullable String destination, @NotNull String doc);
}
