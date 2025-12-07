package com.mipt.team4.cloud_storage_backend.repository.storage;

import static com.mipt.team4.cloud_storage_backend.repository.PostgresRepositoryTest.userRepository;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class StorageRepositoryTest extends BasePostgresTest {
  @Container private static final MinIOContainer MINIO = TestUtils.createMinioContainer();
  private static StorageRepository storageRepository;
  private static UserRepository userRepository;
  private static PostgresConnection postgresConnection;
  private static UUID testUserUuid;

  @BeforeAll
  public static void beforeAll() {
    postgresConnection = TestUtils.createConnection(postgresContainer);
    userRepository = new UserRepository(postgresConnection);
    storageRepository = new StorageRepository(postgresConnection, MINIO.getS3URL());
  }

  @Test
  void shouldAddFile() throws IOException, StorageFileAlreadyExistsException {
    byte[] fileBytes = FileLoader.getInputStream("files/small_file.txt").readAllBytes();
    List<String> tags = new ArrayList<>();
    addTestUser();
    StorageEntity storageEntity = createTestFile();

    storageRepository.addFile(storageEntity, fileBytes);
  }

  @Test
  void shouldGetFile() {}

  @Test
  void fileExists() {}

  @Test
  void startMultipartUpload() {}

  @Test
  void uploadPart() {}

  @Test
  void getFilePathsList() {}

  @Test
  void completeMultipartUpload() {}

  @Test
  void downloadFile() {}

  @Test
  void deleteFile() {}

  @Test
  void updateFile() {}

  @Test
  void downloadFilePart() {}

  @Test
  void testDeleteFile() {}

  @Test
  void addDirectory() {}

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
