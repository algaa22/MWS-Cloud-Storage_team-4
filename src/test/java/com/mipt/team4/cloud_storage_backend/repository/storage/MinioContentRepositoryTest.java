package com.mipt.team4.cloud_storage_backend.repository.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import com.mipt.team4.cloud_storage_backend.utils.TestFiles;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;

@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {MinioContentRepository.class})
@EnableConfigurationProperties(MinioConfig.class)
@Tag("integration")
class MinioContentRepositoryTest {

  private static final MinIOContainer MINIO = TestUtils.createMinioContainer();

  @Autowired private MinioContentRepository repository;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("minio.url", MINIO::getS3URL);
    registry.add("minio.username", MINIO::getUserName);
    registry.add("minio.password", MINIO::getPassword);
  }

  @BeforeAll
  public static void beforeAll() {
    MINIO.start();
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
    assertFileEqualsMinioObject(createTestFile());
  }

  @Test
  public void shouldHardDeleteFile() throws InterruptedException {
    TestFileDto file = createTestFile();

    repository.hardDeleteFile(file.s3Key);

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
      eTags.put(i + 1, repository.uploadPart(uploadID, file.s3Key, i + 1, part));
      offset += PART_SIZE;
    }

    repository.completeMultipartUpload(file.s3Key, uploadID, eTags);

    assertFileEqualsMinioObject(file);
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

  private void assertFileEqualsMinioObject(TestFileDto file) throws IOException {
    try (InputStream downloadStream = repository.downloadObject(file.s3Key)) {
      assertTrue(repository.objectExists(file.s3Key));
      assertArrayEquals(file.data, downloadStream.readAllBytes());
    }
  }

  private record TestFileDto(String s3Key, byte[] data) {}
}
