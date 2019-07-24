package io.datakernel.dao;

import io.datakernel.model.PageView;
import io.datakernel.async.Promise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PageDao {
    Promise<PageView> loadPage(@NotNull String sector, @Nullable String destination, @NotNull String doc);
}
