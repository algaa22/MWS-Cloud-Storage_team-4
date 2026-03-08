package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresConnection implements DatabaseConnection {
  private final javax.sql.DataSource dataSource;

  public boolean isConnected() {
    try (Connection conn = dataSource.getConnection()) {
      return conn != null && !conn.isClosed();
    } catch (SQLException e) {
      return false;
    }
  }

  @Override
  public <T> List<T> executeQuery(String query, List<Object> params, ResultSetMapper<T> mapper) {
    Connection conn = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = conn.prepareStatement(query)) {
      setParameters(statement, params);

      ResultSet resultSet = statement.executeQuery();
      List<T> results = new ArrayList<>();

      while (resultSet.next()) {
        results.add(mapper.map(resultSet));
      }

      return results;
    } catch (SQLException e) {
      throw new DbExecuteQueryException(query, e);
    } finally {
      DataSourceUtils.releaseConnection(conn, dataSource);
    }
  }

  public int executeUpdate(String query, List<Object> params) {
    Connection conn = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = conn.prepareStatement(query)) {
      setParameters(statement, params);

      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new DbExecuteUpdateException(query, e);
    } finally {
      DataSourceUtils.releaseConnection(conn, dataSource);
    }
  }

  private void setParameters(PreparedStatement statement, List<Object> params) throws SQLException {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
    }
  }

  @FunctionalInterface
  public interface ResultSetMapper<T> {

    T map(ResultSet resultSet) throws SQLException;
  }
}
