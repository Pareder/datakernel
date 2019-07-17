package datakernel.dao;

import datakernel.model.PageView;
import io.datakernel.async.Promise;
import io.datakernel.di.annotation.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.concurrent.Executor;

@Inject
public final class PageDatabaseDao implements PageDao {
	@Inject
	private DataSource dataSource;

	@Inject
	private Executor executor;

	@Override
	public Promise<PageView> loadPage(@NotNull String sector, @Nullable String destination, @NotNull String doc) {
		return Promise.ofBlockingCallable(executor, () -> {
			PageView pageView;
			String query = "SELECT content FROM docs " +
					"LEFT JOIN destination dest on docs.destination_path = dest.path " +
					"WHERE docs.sector_path=? AND docs.path=?";
			query += " AND " + (destination != null ? "dest.path=?" : "dest.path is NULL");
			try (Connection connection = dataSource.getConnection()) {
				try (PreparedStatement statement = connection.prepareStatement(query)) {
					int count = 1;
					statement.setString(count++, sector);
					statement.setString(count++, doc);
					if (destination != null) {
						statement.setString(count, destination);
					}
					ResultSet resultSet = statement.executeQuery();
					if (!resultSet.next()) {
						return null;
					}
					pageView = new PageView(sector, resultSet.getString(0),  Collections.emptyMap());
				}

				try (PreparedStatement statement =
							 connection.prepareStatement("SELECT docs.title, docs.path, dest.path, dest.title FROM docs " +
									 "LEFT JOIN destination dest on docs.destination_path = dest.path " +
									 "WHERE docs.sector_path=?;")) {
					statement.setString(1, sector);
					ResultSet resultSet = statement.executeQuery();

					while (resultSet.next()) {
						pageView.putSubParagraph(
								resultSet.getString(1),
								resultSet.getString(2),
								resultSet.getString(4), resultSet.getString(3)
						);
					}
				}
				return pageView;
			}
		});
	}

}
