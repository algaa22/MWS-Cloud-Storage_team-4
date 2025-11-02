package com.mipt.team4.cloud_storage_backend.repository.database;

import org.junit.jupiter.api.Test;

import java.util.List;

class PostgresConnectionTest {
  PostgresConnection postgres;

  @Test
  public void shouldConnect() {
    postgres = PostgresConnection.getInstance();
    postgres.connect();

    for(String str : postgres.executeQuery(
        "SELECT * FROM files WHERE file_size = ?;", List.of(560),
        rs -> rs.getString("owner_id") + " " + rs.getString("storage_path")
        )) {
      System.out.println(str);
    }
  }
}
