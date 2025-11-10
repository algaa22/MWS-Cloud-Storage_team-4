package com.mipt.team4.cloud_storage_backend.repository.database;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PostgresConnectionTest extends AbstractPostgresTest {
  @Test
  public void isConnected_WhenConnected_ReturnsTrue() {
    PostgresConnection postgresConnection = createConnection();

    assertTrue(postgresConnection.isConnected());

    postgresConnection.disconnect();
  }

  @Test
  public void isConnected_WhenDisconnected_ReturnsTrue() {
    PostgresConnection postgresConnection = createConnection();
    postgresConnection.disconnect();

    assertFalse(postgresConnection.isConnected());
  }

  @Test
  public void isConnected_WhenMultipleConnected_ReturnsTrue() {
    PostgresConnection postgresConnection1 = createConnection();
    PostgresConnection postgresConnection2 = createConnection();

    assertTrue(postgresConnection1.isConnected());
    assertTrue(postgresConnection2.isConnected());

    postgresConnection1.disconnect();
    postgresConnection2.disconnect();
  }

  @Test
  public void isConnected_WhenOtherDisconnected_ReturnsTrue() {
    PostgresConnection postgresConnection1 = createConnection();
    PostgresConnection postgresConnection2 = createConnection();
    postgresConnection1.disconnect();

    assertTrue(postgresConnection2.isConnected());

    postgresConnection2.disconnect();
  }
}
