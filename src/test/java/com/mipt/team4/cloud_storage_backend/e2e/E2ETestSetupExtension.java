package com.mipt.team4.cloud_storage_backend.e2e;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class E2ETestSetupExtension implements BeforeAllCallback {

  protected static final String INITIALIZED_KEY = "testcontainers.initialized";
  protected static final PostgreSQLContainer<?> POSTGRES = TestUtils.createPostgresContainer();
  protected static final GenericContainer<?> S3 = TestUtils.createS3Container();

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(ExtensionContext.Namespace.GLOBAL)
        .getOrComputeIfAbsent(
            INITIALIZED_KEY,
            key -> {
              POSTGRES.start();
              S3.start();

              return (AutoCloseable)
                  () -> {
                    S3.stop();
                    POSTGRES.stop();
                  };
            });
  }
}
