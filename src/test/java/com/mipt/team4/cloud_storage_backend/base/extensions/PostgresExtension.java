package com.mipt.team4.cloud_storage_backend.base.extensions;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresExtension implements BeforeAllCallback {
  public static final PostgreSQLContainer<?> POSTGRES = TestUtils.createPostgresContainer();

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(Namespace.GLOBAL)
        .getOrComputeIfAbsent(
            "postgres_init",
            key -> {
              POSTGRES.start();

              System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
              System.setProperty("spring.datasource.username", POSTGRES.getUsername());
              System.setProperty("spring.datasource.password", POSTGRES.getPassword());
              System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

              return (ExtensionContext.Store.CloseableResource) POSTGRES::stop;
            });
  }
}
