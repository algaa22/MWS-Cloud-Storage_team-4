package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.PostgresFileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PostgresRepositoryTest extends BasePostgresTest {
  private static PostgresFileMetadataRepository fileMetadataRepository;
  public static UserRepository userRepository;
  private static PostgresConnection postgresConnection;
  private static UUID testUserUuid;

  @BeforeAll
  protected static void beforeAll() {
    BasePostgresTest.beforeAll();

    postgresConnection = TestUtils.createConnection(postgresContainer);
    fileMetadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    userRepository = new UserRepository(postgresConnection);

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
    StorageEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);

    assertTrue(fileMetadataRepository.fileExists(testFile.getUserId(), testFile.getPath()));
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() {
    assertFalse(fileMetadataRepository.fileExists(UUID.randomUUID(), "asdasd"));
  }

  @Test
  void shouldAddAndGetFile_WithSameContent() throws StorageFileAlreadyExistsException {
    StorageEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);

    Optional<StorageEntity> receivedTestFile =
        fileMetadataRepository.getFile(testUserUuid, testFile.getPath());

    assertTrue(receivedTestFile.isPresent());
    assertTrue(receivedTestFile.get().fullEquals(testFile));
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    assertFalse(fileMetadataRepository.getFile(testUserUuid, "").isPresent());
  }

  @Test
  void shouldThrowException_WhenAddExistentFile() throws StorageFileAlreadyExistsException {
    StorageEntity file = createTestFile();

    fileMetadataRepository.addFile(file);

    assertThrows(
        StorageFileAlreadyExistsException.class, () -> fileMetadataRepository.addFile(file));
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId()
      throws StorageFileAlreadyExistsException, StorageFileNotFoundException {
    StorageEntity testFile = createTestFile();

    fileMetadataRepository.addFile(testFile);
    assertTrue(fileMetadataRepository.fileExists(testFile.getUserId(), testFile.getPath()));

    fileMetadataRepository.deleteFile(testFile.getUserId(), testFile.getPath());
    assertFalse(fileMetadataRepository.fileExists(testFile.getUserId(), testFile.getPath()));
  }

  private static void addTestUser() {
    testUserUuid = UUID.randomUUID();

    try {
      userRepository.addUser(
          new UserEntity(
              testUserUuid, "name", "email", "password", 10, 0, LocalDateTime.now(), true));
    } catch (UserAlreadyExistsException e) {
      throw new RuntimeException(e);
    }
  }

  private static StorageEntity createTestFile() {
    return new StorageEntity(
        UUID.randomUUID(),
        testUserUuid,
            UUID.randomUUID() + ".xml",
        "application/xml",
        "public",
        52,
        false,
        List.of("some xml"),
        false);
  }
}
