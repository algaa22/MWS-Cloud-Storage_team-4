package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.PostgresFileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// TODO: fix
public class PostgresRepositoryTest extends BasePostgresTest {
  private static PostgresFileMetadataRepository fileMetadataRepository;
  private static UserRepository userRepository;
  private static PostgresConnection postgresConnection;
  private static StorageEntity commonFileEntity;
  private static UUID testUserUuid;

  // TODO: корректные ли?

  @BeforeAll
  protected static void beforeAll() {
    BasePostgresTest.beforeAll();

    postgresConnection = TestUtils.createConnection(postgresContainer);
    fileMetadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    userRepository = new UserRepository(postgresConnection);

    addTestUser();

    commonFileEntity = addTestFile();
  }

  @AfterAll
  protected static void afterAll() {
    BasePostgresTest.afterAll();

    postgresConnection.disconnect();
  }

  // TODO: refactor

  @Test
  void fileExists_ShouldReturnTrue_WhenFileExists() {
    assertTrue(
        fileMetadataRepository.fileExists(
            commonFileEntity.getUserId(), commonFileEntity.getPath()));
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() {
    assertFalse(fileMetadataRepository.fileExists(commonFileEntity.getUserId(), "asdasdasd"));
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    assertFalse(fileMetadataRepository.getFile(testUserUuid, "").isPresent());
  }

  @Test
  void shouldThrowException_WhenAddExistentFile() {
    assertThrows(
        StorageFileAlreadyExistsException.class,
        () -> fileMetadataRepository.addFile(commonFileEntity));
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId()
      throws StorageEntityNotFoundException {
    StorageEntity testFileEntity = addTestFile();
    assertTrue(
        fileMetadataRepository.fileExists(testFileEntity.getUserId(), testFileEntity.getPath()));

    fileMetadataRepository.deleteFile(testFileEntity.getUserId(), testFileEntity.getPath());
    assertFalse(
        fileMetadataRepository.fileExists(testFileEntity.getUserId(), testFileEntity.getPath()));
  }

  private static void addTestUser() {
    testUserUuid = UUID.randomUUID();

    try {
      userRepository.addUser(new UserEntity(testUserUuid, "name", "email", "password"));
    } catch (UserAlreadyExistsException e) {
      throw new RuntimeException(e);
    }
  }

  private static StorageEntity addTestFile() {
    try {
      StorageEntity fileEntity =
          new StorageEntity(
              UUID.randomUUID(),
              testUserUuid,
              "file" + UUID.randomUUID(),
              "application/xml",
              "public",
              42L,
              false,
              List.of("some xml"),
              false);

      fileMetadataRepository.addFile(fileEntity);

      return fileEntity;
    } catch (StorageFileAlreadyExistsException e) {
      throw new RuntimeException(e); // TODO: RE в тестах?
    }
  }
}
