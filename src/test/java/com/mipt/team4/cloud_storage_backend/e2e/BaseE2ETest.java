package com.mipt.team4.cloud_storage_backend.e2e;

import com.mipt.team4.cloud_storage_backend.CloudStorageApplication;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServer;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.N;

public abstract class BaseE2ETest {
  private static final PostgreSQLContainer<?> POSTGRES = TestUtils.createPostgresContainer();
  private static final MinIOContainer MINIO = TestUtils.createMinioContainer();

  @BeforeAll
  public static void beforeAll() {
    if (!POSTGRES.isRunning()) POSTGRES.start();
    if (!MINIO.isRunning()) MINIO.start();

    CloudStorageApplication.start();
  }

  @AfterAll
  public static void afterAll() {
    CloudStorageApplication.stop();
  }
}
