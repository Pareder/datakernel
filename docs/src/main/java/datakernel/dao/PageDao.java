package datakernel.dao;

import datakernel.model.PageView;
import io.datakernel.async.Promise;

public interface PageDao {
    Promise<PageView> loadPage(String sector, String destination, String doc);
}
