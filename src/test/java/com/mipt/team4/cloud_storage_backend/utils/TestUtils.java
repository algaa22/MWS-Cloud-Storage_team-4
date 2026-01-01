package com.mipt.team4.cloud_storage_backend.utils;


import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {

  public static PostgreSQLContainer<?> createPostgresContainer() {
    return new PostgreSQLContainer<>("postgres:18.0")
        .withDatabaseName(DatabaseConfig.INSTANCE.getName())
        .withUsername(DatabaseConfig.INSTANCE.getUsername())
        .withPassword(DatabaseConfig.INSTANCE.getPassword());
  }

  public static MinIOContainer createMinioContainer() {
    return new MinIOContainer("minio/minio:latest")
        .withUserName(MinioConfig.INSTANCE.getUsername())
        .withPassword(MinioConfig.INSTANCE.getPassword());
  }

  public static PostgresConnection createConnection(PostgreSQLContainer<?> postgresContainer) {
    PostgresConnection postgresConnection =
        new PostgresConnection(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword());
    postgresConnection.connect();

    return postgresConnection;
  }

  public static HttpRequest.Builder createRequest(String endpoint) {
    return HttpRequest.newBuilder().uri(URI.create(createUriString(endpoint)));
  }

  public static String createUriString(String endpoint) {
    return "http://localhost:" + NettyConfig.INSTANCE.getHttpPort() + endpoint;
  }

  public static JsonNode getRootNodeFromResponse(HttpResponse<String> response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(response.body());
  }

  public static CloseableHttpClient createApacheClient() {
    return HttpClients.custom()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .setConnectionManagerShared(false)
        .build();
  }
}