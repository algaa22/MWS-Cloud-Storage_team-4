package com.mipt.team4.cloud_storage_backend.e2e;

import com.mipt.team4.cloud_storage_backend.CloudStorageApplication;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

// implements BeforeAllCallback, ExtensionContext.Store.CloseableResource
public abstract class BaseIT {
  private static final PostgreSQLContainer<?> POSTGRES = TestUtils.createPostgresContainer();
  private static final MinIOContainer MINIO = TestUtils.createMinioContainer();
  protected static final HttpClient client = HttpClient.newHttpClient();

  private static boolean started = false;

  @BeforeAll
  public static void beforeAll() {
    if (!started) {
      POSTGRES.start();
      MINIO.start();

      CloudStorageApplication.start(POSTGRES.getJdbcUrl(), MINIO.getS3URL());

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    CloudStorageApplication.stop();
                    MINIO.stop();
                    POSTGRES.stop();
                  }));

      started = true;
    }
  }

  @AfterAll
  public static void afterAll() {
    POSTGRES.stop();
    MINIO.stop();

    CloudStorageApplication.stop();
  }
}
