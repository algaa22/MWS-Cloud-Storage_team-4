package com.mipt.team4.cloud_storage_backend.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import com.mipt.team4.cloud_storage_backend.repository.database.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@Import({StorageJpaRepositoryAdapter.class, UserJpaRepositoryAdapter.class})
public class PostgresRepositoryTest extends BasePostgresTest {
  @MockitoBean private NettyServerManager nettyServerManager;

  @Autowired private StorageJpaRepositoryAdapter storageJpaRepositoryAdapter;
  @Autowired private UserJpaRepositoryAdapter userRepository;
  private StorageEntity commonFileEntity;
  private UUID testUserUuid;

  @BeforeEach
  void beforeEach() {
    testUserUuid = addTestUser();
    commonFileEntity = addTestFile(null, "root-file.xml");
  }

  private UUID addTestUser() throws UserAlreadyExistsException {
    UserEntity user =
        UserEntity.builder()
            .username("name")
            .email("test-" + UUID.randomUUID() + "@email.com")
            .passwordHash("password")
            .storageLimit(10737418240L)
            .createdAt(LocalDateTime.now())
            .build();

    userRepository.addUser(user);
    return user.getId();
  }

  private StorageEntity addTestFile(UUID parentId, String name)
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

    storageJpaRepositoryAdapter.addFile(fileEntity);
    return fileEntity;
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
      throws StorageFileNotFoundException, StorageFileAlreadyExistsException {
    String uniqueName = "delete-me-" + UUID.randomUUID();
    StorageEntity testFileEntity = addTestFile(null, uniqueName);
    assertTrue(storageJpaRepositoryAdapter.exists(testFileEntity.getUserId(), null, uniqueName));

    storageJpaRepositoryAdapter.hardDelete(testFileEntity.getUserId(), testFileEntity.getId());
    assertFalse(storageJpaRepositoryAdapter.exists(testFileEntity.getUserId(), null, uniqueName));
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
    storageJpaRepositoryAdapter.addFile(folder);

    StorageEntity childFile = addTestFile(folder.getId(), "child.txt");

    assertTrue(storageJpaRepositoryAdapter.isDescendant(folder.getId(), childFile.getId()));

    assertFalse(storageJpaRepositoryAdapter.isDescendant(childFile.getId(), folder.getId()));
  }
}
