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
  private static PostgresFileMetadataRepository fileMetadataRepository;
  private static PostgresConnection postgresConnection;
  private static UUID testUserUuid;

  // TODO: dodelat

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
  void fileExists_ShouldReturnTrue_WhenFileExists() throws StorageFileAlreadyExistsException {
    FileEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);

    assertTrue(fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() throws StorageFileAlreadyExistsException {
    FileEntity testFile = createTestFile();

    assertTrue(fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));

    fileMetadataRepository.addFile(testFile);
  }

  @Test
  void shouldAddAndGetFile_WithSameContent() throws StorageFileAlreadyExistsException {
    FileEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);

    Optional<FileEntity> receivedTestFile =
        fileMetadataRepository.getFile(testUserUuid, "some/newPath.xml");

    assertTrue(receivedTestFile.isPresent());
    assertTrue(receivedTestFile.get().fullEquals(testFile));
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    assertFalse(fileMetadataRepository.getFile(testUserUuid, "").isPresent());
  }

  @Test
  void shouldThrowException_WhenAddExistentFile() throws StorageFileAlreadyExistsException {
    FileEntity file = createTestFile();

    fileMetadataRepository.addFile(file);

    assertThrows(
        StorageFileAlreadyExistsException.class, () -> fileMetadataRepository.addFile(file));
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId()
      throws StorageFileAlreadyExistsException, StorageFileNotFoundException {
    FileEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);

    assertTrue(fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));

    fileMetadataRepository.deleteFile(testFile.getOwnerId(), testFile.getS3Key());

    assertFalse(fileMetadataRepository.fileExists(testFile.getOwnerId(), testFile.getS3Key()));
  }

  private static void addTestUser() {
    // TODO: добавить нормально, через интерфейс

    testUserUuid = UUID.randomUUID();

    try {
      postgresConnection.executeUpdate(
          "INSERT INTO users (newPath, email, password_hash, username, storage_limit, used_storage, is_active) "
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
        "some/newPath.xml",
        "application/xml",
        "public",
        52,
        false,
        List.of("some xml"));
  }
}
