package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ITUtils {
  private final NettyConfig nettyConfig;

  public ITUtils(NettyConfig nettyConfig) {
    this.nettyConfig = nettyConfig;
  }

  public HttpRequest.Builder createRequest(String endpoint) {
    return HttpRequest.newBuilder().uri(URI.create(createUriString(endpoint)));
  }

  public String createUriString(String endpoint) {
    return "http://localhost:" + nettyConfig.httpPort() + endpoint;
  }

  public JsonNode getRootNodeFromResponse(HttpResponse<String> response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(response.body());
  }

  public CloseableHttpClient createApacheClient() {
    return HttpClients.custom()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .setConnectionManagerShared(false)
        .build();
  }
}
