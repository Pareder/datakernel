package datakernel.dao;

import datakernel.model.PageView;
import io.datakernel.async.Promise;
import io.datakernel.di.annotation.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executor;

@Inject
public final class PageImplDao implements PageDao {
	@Inject
	private DataSource dataSource;

	@Inject
	private Executor executor;

	@Override
	public Promise<PageView> loadPage(String sector, String destination, String doc) {
		return Promise.ofBlockingCallable(executor, () -> {
			PageView pageView = new PageView();
			try (Connection connection = dataSource.getConnection()) {
				try (PreparedStatement statement =
							 connection.prepareStatement("SELECT docs.title, docs.path, dest.title FROM docs " +
									 "LEFT JOIN destination dest on docs.destination_path = dest.path " +
									 "WHERE docs.sector_path=?;")) {
					statement.setString(1, sector);
					ResultSet resultSet = statement.executeQuery();

					while (resultSet.next()) {
						pageView.put(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
					}
				}
				String query = "SELECT content FROM docs " +
						"LEFT JOIN destination dest on docs.destination_path = dest.path " +
						"WHERE docs.sector_path=?";
				query += " AND " + (destination != null ? "dest.path=?" : "dest.path is NULL");
				query += " AND " + (doc != null ? "docs.path=?" : "docs.path is NULL");
				try (PreparedStatement statement = connection.prepareStatement(query)) {
					int count = 1;
					statement.setString(count++, sector);
					if (destination != null) {
						statement.setString(count++, destination);
					}
					if (doc != null) {
						statement.setString(count, doc);
					}
					ResultSet resultSet = statement.executeQuery();
					return resultSet.next() ?
							pageView.setPath(sector)
									.setContent(resultSet.getString(1)) :
							null;
				}
			}
		});
	}

}
