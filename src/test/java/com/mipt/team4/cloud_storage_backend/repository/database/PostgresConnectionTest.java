package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PostgresConnectionTest extends BasePostgresTest {
  @Test
  public void isConnected_WhenConnected_ReturnsTrue() {
    PostgresConnection postgresConnection = TestUtils.createConnection(postgresContainer);

    assertTrue(postgresConnection.isConnected());

    postgresConnection.disconnect();
  }

  @Test
  public void isConnected_WhenDisconnected_ReturnsTrue() {
    PostgresConnection postgresConnection = TestUtils.createConnection(postgresContainer);
    postgresConnection.disconnect();

    assertFalse(postgresConnection.isConnected());
  }

  @Test
  public void isConnected_WhenMultipleConnected_ReturnsTrue() {
    PostgresConnection postgresConnection1 = TestUtils.createConnection(postgresContainer);
    PostgresConnection postgresConnection2 = TestUtils.createConnection(postgresContainer);

    assertTrue(postgresConnection1.isConnected());
    assertTrue(postgresConnection2.isConnected());

    postgresConnection1.disconnect();
    postgresConnection2.disconnect();
  }

  @Test
  public void isConnected_WhenOtherDisconnected_ReturnsTrue() {
    PostgresConnection postgresConnection1 = TestUtils.createConnection(postgresContainer);
    PostgresConnection postgresConnection2 = TestUtils.createConnection(postgresContainer);
    postgresConnection1.disconnect();

    assertTrue(postgresConnection2.isConnected());

    postgresConnection2.disconnect();
  }
}
