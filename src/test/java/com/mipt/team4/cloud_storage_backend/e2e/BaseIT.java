package com.mipt.team4.cloud_storage_backend.e2e;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(E2ETestSetupExtension.class)
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
    registry.add("database.url", E2ETestSetupExtension.POSTGRES::getJdbcUrl);
    registry.add("database.username", E2ETestSetupExtension.POSTGRES::getUsername);
    registry.add("database.password", E2ETestSetupExtension.POSTGRES::getPassword);
    registry.add("minio.url", E2ETestSetupExtension.MINIO::getS3URL);
    registry.add("minio.username", E2ETestSetupExtension.MINIO::getUserName);
    registry.add("minio.password", E2ETestSetupExtension.MINIO::getPassword);
    registry.add(
        "storage.auth.jwt-secret-key",
        () -> "Y29tZS12ZXJ5LWxvbmctYW5kLXNlY3VyZS10ZXN0LXNlY3V0ZS1rZXktMzItY2hhcnM=");
  }
}
