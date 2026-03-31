package com.mipt.team4.cloud_storage_backend.repository.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig.S3;
import com.mipt.team4.cloud_storage_backend.utils.TestConstants;
import com.mipt.team4.cloud_storage_backend.utils.TestFiles;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
      S3ContentRepository.class,
      S3Wrapper.class,
      S3ContentRepositoryTest.S3TestConfig.class
    })
@EnableConfigurationProperties(StorageConfig.class)
@Tag("integration")
class S3ContentRepositoryTest {

  @TestConfiguration
  static class S3TestConfig {
    @Bean
    public S3Client s3Client(StorageConfig config) {
      StorageConfig.S3 s3Props = config.s3();

      return S3Client.builder()
          .endpointOverride(java.net.URI.create(s3Props.url()))
          .region(software.amazon.awssdk.regions.Region.of(s3Props.region()))
          .credentialsProvider(
              software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                  software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                      s3Props.accessKey(), s3Props.secretKey())))
          .forcePathStyle(true)
          .build();
    }
  }

  private static final GenericContainer<?> S3 = TestUtils.createS3Container();

  @Autowired private S3ContentRepository repository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    String s3Url =
        "http://"
            + StorageConfig.S3.getHost()
            + ":"
            + StorageConfig.S3.getMappedPort(TestConstants.S3_INTERNAL_PORT);

    registry.add("storage.s3.url", () -> s3Url);
    registry.add("storage.s3.access-key", () -> "test-key");
    registry.add("storage.s3.secret-key", () -> "test-secret");
  }

  @BeforeAll
  public static void beforeAll() {
    S3.start();
  }

  @Test
  public void bucketExists_shouldReturnFalse_WhenBucketNotExists() {
    assertFalse(repository.bucketExists("non-existent-bucket"));
  }

  @Test
  public void shouldCreateUnexistentBucket() {
    assertTrue(repository.bucketExists(createTestBucket()));
  }

  @Test
  public void shouldPutAndDownloadObject() throws IOException {
    assertFileEqualsS3Object(createTestFile());
  }

  @Test
  public void shouldHardDeleteFile() throws InterruptedException {
    TestFileDto file = createTestFile();

    repository.hardDelete(file.s3Key);

    boolean deleted = false;
    for (int i = 0; i < 10; i++) {
      if (!repository.objectExists(file.s3Key)) {
        deleted = true;
        break;
      }

      Thread.sleep(50);
    }

    assertTrue(deleted);
  }

  @Test
  public void shouldDoMultipartUpload() throws IOException {
    TestFileDto file = createTestFile();
    String uploadID = repository.startMultipartUpload(file.s3Key);

    final int PART_SIZE = 1024 * 1024 * 5;
    int partCount = Math.ceilDiv(file.data.length, PART_SIZE);
    int offset = 0;

    Map<Integer, String> eTags = new LinkedHashMap<>(partCount);

    for (int i = 0; i < partCount; i++) {
      int end = Math.min(offset + PART_SIZE, file.data.length);
      byte[] part = Arrays.copyOfRange(file.data, offset, end);

      try (InputStream inputStream = new ByteArrayInputStream(part)) {
        eTags.put(
            i + 1, repository.uploadPart(uploadID, file.s3Key, i + 1, inputStream, part.length));
      }

      offset += PART_SIZE;
    }

    repository.completeMultipartUpload(file.s3Key, uploadID, eTags);

    assertFileEqualsS3Object(file);
  }

  private String createTestBucket() {
    String bucketName = "bucket-" + UUID.randomUUID();

    repository.createBucket(bucketName);

    return bucketName;
  }

  private TestFileDto createTestFile() {
    TestFileDto file = new TestFileDto("file" + UUID.randomUUID(), TestFiles.SMALL_FILE.getData());
    repository.putObject(file.s3Key, file.data);

    return file;
  }

  private void assertFileEqualsS3Object(TestFileDto file) throws IOException {
    try (InputStream downloadStream = repository.downloadObject(file.s3Key)) {
      assertTrue(repository.objectExists(file.s3Key));
      assertArrayEquals(file.data, downloadStream.readAllBytes());
    }
  }

  private record TestFileDto(String s3Key, byte[] data) {}
}
