package com.mipt.team4.cloud_storage_backend.e2e;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2ETestSetupExtension.class)
public class BaseIT {

  protected static final CloseableHttpClient apacheClient =
      HttpClients.custom()
          .setConnectionManager(new PoolingHttpClientConnectionManager())
          .setConnectionManagerShared(false)
          .build();
  protected static final HttpClient client =
      HttpClient.newBuilder().version(Version.HTTP_1_1).build();
}
