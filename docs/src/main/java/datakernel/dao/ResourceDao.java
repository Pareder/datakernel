package datakernel.dao;

import java.io.IOException;

public interface ResourceDao {
	String getResource(String resourceName) throws IOException;
}
