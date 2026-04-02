package com.mipt.team4.cloud_storage_backend.base;

import com.mipt.team4.cloud_storage_backend.base.extensions.PostgresExtension;
import com.mipt.team4.cloud_storage_backend.base.extensions.S3Extension;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith({PostgresExtension.class, S3Extension.class})
public abstract class BaseIT {
  protected static final CloseableHttpClient apacheClient =
      HttpClients.custom()
          .setConnectionManager(new PoolingHttpClientConnectionManager())
          .setConnectionManagerShared(false)
          .build();
  protected static final HttpClient client =
      HttpClient.newBuilder().version(Version.HTTP_1_1).build();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "storage.auth.jwt-secret-key",
        () -> "Y29tZS12ZXJ5LWxvbmctYW5kLXNlY3VyZS10ZXN0LXNlY3V0ZS1rZXktMzItY2hhcnM=");

    registry.add("storage.health-check.interval-seconds", () -> 10);
    registry.add("storage.health-check.db-timeout-seconds", () -> 2);
    registry.add("storage.health-check.s3-timeout-seconds", () -> 2);
    registry.add("storage.health-check.min-free-memory-percent", () -> 0.10);
  }
}
