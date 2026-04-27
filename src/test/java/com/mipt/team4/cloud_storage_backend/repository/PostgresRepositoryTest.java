package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.base.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
public class PostgresRepositoryTest extends BasePostgresTest {
  @Autowired private StorageJpaRepositoryAdapter storageJpaRepositoryAdapter;
  @Autowired private UserJpaRepositoryAdapter userRepository;
  @Autowired private EntityManager entityManager;

  private StorageEntity commonFileEntity;
  private UUID testUserUuid;

  @BeforeEach
  void beforeEach() {
    testUserUuid = addTestUser();
    commonFileEntity = addTestFile(null, "root-file.xml");
  }

  @Test
  void fileExists_ShouldReturnTrue_WhenFileExists() {
    assertTrue(
        storageJpaRepositoryAdapter.exists(
            commonFileEntity.getUserId(),
            commonFileEntity.getParentId(),
            commonFileEntity.getName()));
  }

  @Test
  void fileExists_ShouldReturnFalse_WhenFileNotFound() {
    assertFalse(
        storageJpaRepositoryAdapter.exists(
            commonFileEntity.getUserId(), null, "non-existent-file"));
  }

  @Test
  void shouldReturnNull_WhenGetNonexistentFile() {
    assertTrue(storageJpaRepositoryAdapter.get(testUserUuid, null, "non-existent-path").isEmpty());
  }

  @Test
  void shouldAddAndDeleteFile_WithSameId()
      throws FileNotFoundException, FileAlreadyExistsException {
    String uniqueName = "delete-me-" + UUID.randomUUID();
    StorageEntity testFileEntity = addTestFile(null, uniqueName);
    assertTrue(storageJpaRepositoryAdapter.exists(testFileEntity.getUserId(), null, uniqueName));

    storageJpaRepositoryAdapter.hardDelete(testFileEntity.getUserId(), testFileEntity.getId());
    assertFalse(storageJpaRepositoryAdapter.exists(testFileEntity.getUserId(), null, uniqueName));
  }

  @Test
  public void shouldSoftDeleteFile() {
    StorageEntity testFileEntity = addTestFile(null, "test-name");
    assertEntityIsActive(testFileEntity);

    storageJpaRepositoryAdapter.softDelete(
        testFileEntity.getUserId(), testFileEntity.getId(), testFileEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);

    assertEntityIsDeleted(testFileEntity);
  }

  @Test
  public void shouldSoftDeleteDirectory() {
    StorageEntity testDirectoryEntity = addTestDirectory(null, "test-dir");

    StorageEntity testFileEntity = addTestFile(testDirectoryEntity.getId(), "test-name");
    assertFalse(testFileEntity.isDeleted());

    StorageEntity otherFileEntity = addTestFile(null, "other-name");

    storageJpaRepositoryAdapter.softDelete(
        testDirectoryEntity.getUserId(),
        testDirectoryEntity.getId(),
        testDirectoryEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);
    testDirectoryEntity = refresh(testDirectoryEntity);
    otherFileEntity = refresh(otherFileEntity);

    assertEntityIsDeleted(testFileEntity);
    assertEntityIsDeleted(testDirectoryEntity);
    assertEntityIsActive(otherFileEntity);
  }

  @Test
  public void shouldRestoreFile() {
    StorageEntity testFileEntity = addTestFile(null, "test-name");
    assertEntityIsActive(testFileEntity);
    storageJpaRepositoryAdapter.softDelete(
        testFileEntity.getUserId(), testFileEntity.getId(), testFileEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);

    assertEntityIsDeleted(testFileEntity);
    storageJpaRepositoryAdapter.restore(
        testFileEntity.getUserId(), testFileEntity.getId(), testFileEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);

    assertEntityIsActive(testFileEntity);
  }

  @Test
  public void shouldRestoreDirectory() {
    StorageEntity testDirectoryEntity = addTestDirectory(null, "test-dir");

    StorageEntity testFileEntity = addTestFile(testDirectoryEntity.getId(), "test-name");
    assertEntityIsActive(testFileEntity);

    StorageEntity otherFileEntity = addTestFile(null, "other-name");

    storageJpaRepositoryAdapter.softDelete(
        testDirectoryEntity.getUserId(),
        testDirectoryEntity.getId(),
        testDirectoryEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);
    testDirectoryEntity = refresh(testDirectoryEntity);
    otherFileEntity = refresh(otherFileEntity);

    assertEntityIsDeleted(testFileEntity);
    assertEntityIsDeleted(testDirectoryEntity);
    assertEntityIsActive(otherFileEntity);

    storageJpaRepositoryAdapter.restore(
        testDirectoryEntity.getUserId(),
        testDirectoryEntity.getId(),
        testDirectoryEntity.isDirectory());
    storageJpaRepositoryAdapter.softDelete(
        otherFileEntity.getUserId(), otherFileEntity.getId(), otherFileEntity.isDirectory());

    testFileEntity = refresh(testFileEntity);
    testDirectoryEntity = refresh(testDirectoryEntity);
    otherFileEntity = refresh(otherFileEntity);

    assertEntityIsActive(testFileEntity);
    assertEntityIsActive(testDirectoryEntity);
    assertEntityIsDeleted(otherFileEntity);
  }

  @Test
  void hierarchyTest_ShouldDetectDescendant() throws FileAlreadyExistsException {
    StorageEntity folder =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(testUserUuid)
            .name("parent-folder")
            .isDirectory(true)
            .status(com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus.READY)
            .build();
    storageJpaRepositoryAdapter.addFile(folder);

    StorageEntity childFile = addTestFile(folder.getId(), "child.txt");

    assertTrue(storageJpaRepositoryAdapter.isDescendant(folder.getId(), childFile.getId()));
    assertFalse(storageJpaRepositoryAdapter.isDescendant(childFile.getId(), folder.getId()));
  }

  private UUID addTestUser() throws UserAlreadyExistsException {
    UserEntity user =
        UserEntity.builder()
            .username("name")
            .email("test-" + UUID.randomUUID() + "@email.com")
            .passwordHash("password")
            .createdAt(LocalDateTime.now())
            .build();

    userRepository.addUser(user);
    return user.getId();
  }

  private StorageEntity addTestFile(UUID parentId, String name) throws FileAlreadyExistsException {
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

    storageJpaRepositoryAdapter.addFile(fileEntity);
    return fileEntity;
  }

  private StorageEntity addTestDirectory(UUID parentId, String name)
      throws FileAlreadyExistsException {
    StorageEntity directoryEntity =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(testUserUuid)
            .parentId(parentId)
            .name(name)
            .mimeType("application/directory")
            .size(42L)
            .isDirectory(true)
            .status(com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus.READY)
            .tags(List.of("some directory"))
            .build();

    storageJpaRepositoryAdapter.addFile(directoryEntity);
    return directoryEntity;
  }

  private void assertEntityIsDeleted(StorageEntity entity) {
    assertTrue(entity.isDeleted());
    assertNotNull(entity.getDeletedAt());
  }

  private void assertEntityIsActive(StorageEntity entity) {
    assertFalse(entity.isDeleted());
    assertNull(entity.getDeletedAt());
  }

  private StorageEntity refresh(StorageEntity entity) {
    StorageEntity managedEntity = entityManager.find(StorageEntity.class, entity.getId());

    if (managedEntity != null) {
      entityManager.refresh(managedEntity);
      return managedEntity;
    }

    throw new AssertionError("Entity with ID " + entity.getId() + " not found even in DB");
  }
}
