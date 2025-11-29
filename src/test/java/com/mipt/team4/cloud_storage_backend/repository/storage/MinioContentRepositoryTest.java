package com.mipt.team4.cloud_storage_backend.repository.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.storage.BucketAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioContentRepositoryTest {
  @Container private static final MinIOContainer MINIO = TestUtils.createMinioContainer();
  private static MinioContentRepository repository;

  @BeforeAll
  public static void beforeAll() {
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

    repository.putObject("OwnerID/FileID", fileBytes, "text/plain");
  }

  @Test
  public void shouldDownloadFile() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();

    repository.putObject("OwnerID/FileID2", fileBytes, "text/plain");

    byte[] result = repository.downloadFile("OwnerID/FileID2");
    assertArrayEquals(fileBytes, result);
  }

  @Test
  public void shouldHardDeleteFile() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();

    repository.putObject("OwnerID/FileID2", fileBytes, "text/plain");

    repository.hardDeleteFile("OwnerID/FileID2");

    assertThrows(
        RuntimeException.class,
        () -> repository.downloadFile("OwnerID/FileID2"));
  }

  @Test
  public void ShouldDoMultipartUpload() throws IOException {
    byte[] fileBytes = FileLoader.getInputStream("files/big_file.txt").readAllBytes();

    String S3Key = "OwnerID/FileID3";
    multipartUpload(fileBytes, S3Key);
  }

  @Test
    public void shouldDoMultipartDownload() throws IOException {
      byte[] fileBytes = FileLoader.getInputStream("files/big_file.txt").readAllBytes();

      String S3Key = "OwnerID/FileID4";
      multipartUpload(fileBytes, S3Key);

      int offset = 0;
      int partSize = 1024 * 1024 * 5;
      int partCount = Math.ceilDiv(fileBytes.length, partSize);
      int notDownloadedYet = fileBytes.length;
      byte[] result = new byte[fileBytes.length];
      int actualPartSize;

      for(int i = 0; i < partCount; i++) {
          actualPartSize = (notDownloadedYet - partSize > 0) ? partSize : notDownloadedYet;
          byte[] part = repository.downloadFilePart(S3Key, offset, actualPartSize);
          System.arraycopy(part, 0, result, offset, actualPartSize);
          offset += actualPartSize;
          notDownloadedYet -= actualPartSize;
      }

      assertArrayEquals(fileBytes, result);
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
