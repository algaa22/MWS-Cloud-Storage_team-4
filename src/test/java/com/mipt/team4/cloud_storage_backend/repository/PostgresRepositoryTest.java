package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
public class PostgresRepositoryTest extends BasePostgresTest {

  private static FileMetadataRepository fileMetadataRepository;
  private static UserRepository userRepository;
  private static PostgresConnection postgresConnection;
  private static StorageEntity commonFileEntity;
  private static UUID testUserUuid;

  @BeforeAll
  protected static void beforeAll() {
    BasePostgresTest.beforeAll();

    postgresConnection = TestUtils.createConnection(postgresContainer);
    fileMetadataRepository = new FileMetadataRepository(postgresConnection);
    userRepository = new UserRepository(postgresConnection);

    try {
      testUserUuid = addTestUser();
      commonFileEntity = addTestFile(null, "root-file.xml");
    } catch (UserAlreadyExistsException | StorageFileAlreadyExistsException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  protected static void afterAll() {
    BasePostgresTest.afterAll();
    if (postgresConnection != null) {
      postgresConnection.disconnect();
    }
  }

  private static UUID addTestUser() throws UserAlreadyExistsException {
    UUID uuid = UUID.randomUUID();

    userRepository.addUser(
        UserEntity.builder()
            .id(uuid)
            .name("name")
            .email("email")
            .passwordHash("password")
            .storageLimit((long) 1e10)
            .createdAt(LocalDateTime.now())
            .build());

    return uuid;
  }

  private static StorageEntity addTestFile(UUID parentId, String name)
      throws StorageFileAlreadyExistsException {
    StorageEntity fileEntity =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(testUserUuid)
            .parentId(parentId)
            .name(name)
            .mimeType("application/xml")
            .size(42L)
            .isDirectory(false)
            .status(com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus.READY)
            .tags(List.of("some xml"))
            .build();

    fileMetadataRepository.addFile(fileEntity);

    return fileEntity;
  }

  @Test
  void fileExists_ShouldReturnTrue_WhenFileExists() {
    assertTrue(
        fileMetadataRepository.fileExists(
            commonFileEntity.getUserId(),
            commonFileEntity.getParentId(),
            commonFileEntity.getName()));
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() {
    assertFalse(
        fileMetadataRepository.fileExists(commonFileEntity.getUserId(), null, "non-existent-file"));
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    assertTrue(fileMetadataRepository.getFile(testUserUuid, null, "non-existent-path").isEmpty());
  }

  @Test
  void shouldThrowException_WhenAddExistentFile() {
    assertThrows(
        StorageFileAlreadyExistsException.class,
        () -> fileMetadataRepository.addFile(commonFileEntity));
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId()
      throws StorageFileNotFoundException, StorageFileAlreadyExistsException {
    String uniqueName = "delete-me-" + UUID.randomUUID();
    StorageEntity testFileEntity = addTestFile(null, uniqueName);
    assertTrue(fileMetadataRepository.fileExists(testFileEntity.getUserId(), null, uniqueName));

    fileMetadataRepository.deleteFile(testFileEntity.getUserId(), null, uniqueName);
    assertFalse(fileMetadataRepository.fileExists(testFileEntity.getUserId(), null, uniqueName));
  }

  @Test
  void hierarchyTest_ShouldDetectDescendant() throws StorageFileAlreadyExistsException {
    StorageEntity folder =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(testUserUuid)
            .name("parent-folder")
            .isDirectory(true)
            .status(com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus.READY)
            .build();
    fileMetadataRepository.addFile(folder);

    StorageEntity childFile = addTestFile(folder.getId(), "child.txt");

    assertTrue(fileMetadataRepository.isDescendant(folder.getId(), childFile.getId()));

    assertFalse(fileMetadataRepository.isDescendant(childFile.getId(), folder.getId()));
  }
}
