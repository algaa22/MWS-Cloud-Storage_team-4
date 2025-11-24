package com.mipt.team4.cloud_storage_backend.e2e;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.CloudStorageApplication;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseE2ETest {
  private static final PostgreSQLContainer<?> POSTGRES = TestUtils.createPostgresContainer();
  private static final MinIOContainer MINIO = TestUtils.createMinioContainer();
  protected static final HttpClient client = HttpClient.newHttpClient();

  @BeforeAll
  public static void beforeAll() {
    // TODO: several tests?
    POSTGRES.start();
    MINIO.start();

    CloudStorageApplication.start(POSTGRES.getJdbcUrl(), MINIO.getS3URL());
  }

  @AfterAll
  public static void afterAll() {
    POSTGRES.stop();
    MINIO.stop();

    CloudStorageApplication.stop();
  }
}
