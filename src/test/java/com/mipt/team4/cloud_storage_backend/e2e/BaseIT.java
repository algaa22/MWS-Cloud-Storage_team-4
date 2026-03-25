package com.mipt.team4.cloud_storage_backend.e2e;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(E2ETestSetupExtension.class)
@ActiveProfiles("test")
public class BaseIT {
  protected static final CloseableHttpClient apacheClient =
      HttpClients.custom()
          .setConnectionManager(new PoolingHttpClientConnectionManager())
          .setConnectionManagerShared(false)
          .build();
  protected static final HttpClient client =
      HttpClient.newBuilder().version(Version.HTTP_1_1).build();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", E2ETestSetupExtension.POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", E2ETestSetupExtension.POSTGRES::getUsername);
    registry.add("spring.datasource.password", E2ETestSetupExtension.POSTGRES::getPassword);

    registry.add(
        "storage.s3.url",
        () ->
            "http://"
                + E2ETestSetupExtension.S3.getHost()
                + ":"
                + E2ETestSetupExtension.S3.getMappedPort(8333));
    registry.add("storage.s3.access-key", () -> "test-key");
    registry.add("storage.s3.secret-key", () -> "test-secret");
    registry.add("storage.s3.user-data-bucket.name", () -> "my-test-bucket");
    registry.add(
        "storage.auth.jwt-secret-key",
        () -> "Y29tZS12ZXJ5LWxvbmctYW5kLXNlY3VyZS10ZXN0LXNlY3V0ZS1rZXktMzItY2hhcnM=");

    registry.add("storage.health-check.interval-seconds", () -> 10);
    registry.add("storage.health-check.db-timeout-seconds", () -> 2);
    registry.add("storage.health-check.s3-timeout-seconds", () -> 2);
    registry.add("storage.health-check.min-free-memory-percent", () -> 0.10);
  }
}
