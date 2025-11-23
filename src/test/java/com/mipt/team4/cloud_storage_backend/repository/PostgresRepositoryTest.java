package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.PostgresFileMetadataRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PostgresRepositoryTest extends BasePostgresTest {
  private static final String UNEXPECTED_DB_EXCEPTION_MESSAGE =
      "Database exception should not be thrown";

  private static PostgresFileMetadataRepository fileMetadataRepository;
  private static PostgresConnection postgresConnection;
  private static UUID testUserUuid;

  @BeforeAll
  protected static void beforeAll() {
    BasePostgresTest.beforeAll();

    postgresConnection = TestUtils.createConnection(postgresContainer);
    fileMetadataRepository = new PostgresFileMetadataRepository(postgresConnection);

    addTestUser();
  }

  @AfterAll
  protected static void afterAll() {
    BasePostgresTest.afterAll();

    postgresConnection.disconnect();
  }

  // TODO: refactor

  @Test
  void fileExists_ShouldReturnTrue_WhenFileExists() {
    FileEntity testFile = createTestFile();

    try {
      fileMetadataRepository.addFile(testFile);

      assertTrue(
          fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));
    } catch (StorageFileAlreadyExistsException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() {
    FileEntity testFile = createTestFile();

    try {
      assertTrue(
          fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));

      fileMetadataRepository.addFile(testFile);
    } catch (StorageFileAlreadyExistsException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

  @Test
  void shouldAddAndGetFile_WithSameContent() {
    FileEntity testFile = createTestFile();

    try {
      fileMetadataRepository.addFile(testFile);

      Optional<FileEntity> receivedTestFile =
          fileMetadataRepository.getFile(testUserUuid, "some/path.xml");

      assertTrue(receivedTestFile.isPresent());
      assertTrue(receivedTestFile.get().fullEquals(testFile));
    } catch (StorageFileAlreadyExistsException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    try {
      assertFalse(fileMetadataRepository.getFile(testUserUuid, "").isPresent());
    } catch (DbExecuteQueryException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

  @Test
  void shouldThrowException_WhenAddExistentFile() {
    FileEntity file = createTestFile();

    try {
      fileMetadataRepository.addFile(file);
    } catch (StorageFileAlreadyExistsException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }

    assertThrows(
        StorageFileAlreadyExistsException.class, () -> fileMetadataRepository.addFile(file));
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId() {
    FileEntity testFile = createTestFile();

    try {
      fileMetadataRepository.addFile(testFile);

      assertTrue(
          fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));

      fileMetadataRepository.deleteFile(testFile.getOwnerId(), testFile.getS3Key());

      assertFalse(
          fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));
    } catch (StorageFileAlreadyExistsException | StorageFileNotFoundException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

  private static void addTestUser() {
    // TODO: добавить нормально, через интерфейс

    testUserUuid = UUID.randomUUID();

    try {
      postgresConnection.executeUpdate(
          "INSERT INTO users (path, email, password_hash, username, storage_limit, used_storage, is_active) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?)",
          List.of(
              testUserUuid, "test@example.com", "password", "test_user", 10737418240L, 0, true));
    } catch (DbExecuteUpdateException e) {
      throw new RuntimeException(e);
    }
  }

  private static FileEntity createTestFile() {
    return new FileEntity(
        UUID.randomUUID(),
        testUserUuid,
        "some/path.xml",
        "application/xml",
        "public",
        52,
        false,
        List.of("some xml"));
  }
}
