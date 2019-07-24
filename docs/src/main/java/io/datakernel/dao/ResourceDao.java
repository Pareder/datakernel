package io.datakernel.dao;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface ResourceDao {
	@NotNull
	String getResource(String resourceName) throws IOException;
}
