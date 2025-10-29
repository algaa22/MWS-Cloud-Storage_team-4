package com.mipt.team4.cloud_storage_backend.repository.database;

import org.junit.jupiter.api.Test;

import java.util.List;

class PostgresConnectionTest {
  PostgresConnection postgresConnection = new PostgresConnection();
  @Test
  public void shouldConnect() {
    postgresConnection.connect();


    for(String str : postgresConnection.executeQuery(
        "SELECT * FROM files WHERE file_size = ?;", List.of(560),
        rs -> rs.getString("owner_id") + " " + rs.getString("storage_path")
        )) {
      System.out.println(str);
    }
  }
}
