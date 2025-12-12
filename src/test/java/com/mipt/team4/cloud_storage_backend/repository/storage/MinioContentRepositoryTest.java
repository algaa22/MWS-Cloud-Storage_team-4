package com.mipt.team4.cloud_storage_backend.repository.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.storage.BucketAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;

class MinioContentRepositoryTest {
  private static final MinIOContainer MINIO = TestUtils.createMinioContainer();
  private static MinioContentRepository repository;

  // TODO: тесты некорректные

  @BeforeAll
  public static void beforeAll() {
    MINIO.start();
    repository = new MinioContentRepository(MINIO.getS3URL());
  }

  @Test
  public void shouldCreateBucket() throws BucketAlreadyExistsException {
    repository.createBucket("test");

    assertTrue(repository.bucketExists("test"));
  }

  @Test
  public void shouldThrowBucketAlreadyExistsException() throws BucketAlreadyExistsException {
    repository.createBucket("test2");

    assertThrows(
            BucketAlreadyExistsException.class,
            () -> repository.createBucket("test2"));
  }

  @Test
  public void shouldPutObject() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();

    repository.putObject("OwnerID/FileID", fileBytes);
  }

  @Test
  public void shouldDownloadFile() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();

    repository.putObject("key", fileBytes);

    try (InputStream result = repository.downloadFile("key")) {
      assertArrayEquals(fileBytes, result.readAllBytes());
    }
  }

  @Test
  public void shouldHardDeleteFile() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();

    repository.putObject("OwnerID/FileID2", fileBytes);

    repository.hardDeleteFile("OwnerID/FileID2");

    assertThrows(
            RuntimeException.class,
            () -> repository.downloadFile("OwnerID/FileID2"));
  }

  @Test
  public void shouldDoMultipartUpload() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/big_file.txt").readAllBytes();

    String S3Key = "OwnerID/FileID3";
    multipartUpload(fileBytes, S3Key);
  }

  private void multipartUpload(byte[] fileBytes, String S3Key) {
    String uploadID = repository.startMultipartUpload(S3Key);

    int partSize = 1024 * 1024 * 5;
    int partCount = Math.ceilDiv(fileBytes.length, partSize);

    int offset = 0;
    Map<Integer, String> etags = new LinkedHashMap<>(partCount);

    for (int i = 0; i < partCount; i++) {
      int end = Math.min(offset + partSize, fileBytes.length);
      byte[] part = Arrays.copyOfRange(fileBytes, offset, end);
      etags.put(i+1, repository.uploadPart(uploadID, S3Key, i+1, part));
      offset += partSize;
    }

    repository.completeMultipartUpload(S3Key, uploadID, etags);
  }
}
