package io.datakernel.dao;

import io.datakernel.model.PageView;
import io.datakernel.async.Promise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface PageDao {
    Promise<PageView> loadPage(@NotNull String url);
    Promise<PageView> loadPage(@NotNull Path resource);
}
