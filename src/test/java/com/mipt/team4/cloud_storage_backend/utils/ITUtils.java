package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ITUtils {
  private final NettyConfig nettyConfig;

  public String fillQuery(String endpoint, Object... objects) {
    Object[] encodedObjects =
        Arrays.stream(objects).map(this::toQueryParam).toArray();

    return endpoint.formatted(encodedObjects);
  }

  public String toQueryParam(Object object) {
    return URLEncoder.encode(object.toString(), StandardCharsets.UTF_8);
  }

  public HttpRequest.Builder createRequest(String endpoint) {
    return HttpRequest.newBuilder().uri(URI.create(createUriString(endpoint)));
  }

  public String createUriString(String endpoint) {
    return "http://localhost:" + nettyConfig.httpPort() + endpoint;
  }

  public UUID extractIdFromResponse(HttpResponse<String> response) throws IOException {
    return UUID.fromString(getRootNodeFromResponse(response).get("id").asText());
  }

  public UUID extractIdFromBody(String body) throws IOException {
    return UUID.fromString(getRootNodeFromBody(body).get("id").asText());
  }

  public JsonNode getRootNodeFromResponse(HttpResponse<String> response) throws IOException {
    return getRootNodeFromBody(response.body());
  }

  public JsonNode getRootNodeFromBody(String body) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(body);
  }

  public CloseableHttpClient createApacheClient() {
    return HttpClients.custom()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .setConnectionManagerShared(false)
        .build();
  }
}
