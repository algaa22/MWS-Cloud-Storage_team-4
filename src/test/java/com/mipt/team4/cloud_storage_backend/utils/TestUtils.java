package com.mipt.team4.cloud_storage_backend.utils;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class TestUtils {
  public static PostgreSQLContainer<?> createPostgresContainer() {
    return new PostgreSQLContainer<>("postgres:18.0")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_pass");
  }

  public static GenericContainer<?> createS3Container() {
    return new GenericContainer<>("chrislusf/seaweedfs:latest")
        .withExposedPorts(TestConstants.S3_INTERNAL_PORT)
        .withCommand(
            "server", "-dir=/data", "-s3", "-s3.port=8333", "-s3.config=/etc/seaweedfs/s3.json")
        .withClasspathResourceMapping("s3.json", "/etc/seaweedfs/s3.json", BindMode.READ_ONLY)
        .waitingFor(Wait.forHttp("/").forStatusCode(200).forStatusCode(403).forPort(TestConstants.S3_INTERNAL_PORT));
  }
}
