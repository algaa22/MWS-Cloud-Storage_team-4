package com.mipt.team4.cloud_storage_backend.repository.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.repository.database.AbstractPostgresTest;
import com.mipt.team4.cloud_storage_backend.repository.storage.PostgresFileMetadataRepository;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PostgresRepositoryTest extends AbstractPostgresTest {
  private static final String UNEXPECTED_DB_EXCEPTION_MESSAGE =
      "Database exception should not be thrown";

  private static PostgresFileMetadataRepository fileMetadataRepository;
  private static PostgresConnection postgresConnection;
  private static UUID testUserUuid;

  @BeforeAll
  protected static void beforeAll() {
    AbstractPostgresTest.beforeAll();

    postgresConnection = createConnection();
    fileMetadataRepository = new PostgresFileMetadataRepository(postgresConnection);

    addTestUser();
  }

  @AfterAll
  protected static void afterAll() {
    AbstractPostgresTest.afterAll();

    postgresConnection.disconnect();
  }

  @Test
  void shouldDetectCreatedFile() {
      FileEntity testFile = createTestFile();

      try {
          fileMetadataRepository.addFile(testFile);

          boolean doesExist = fileMetadataRepository.doesFileExist(testFile.getOwnerId(), testFile.getStoragePath());

          assertTrue(doesExist);
      } catch (DbExecuteQueryException | DbExecuteUpdateException | FileAlreadyExistsException e) {
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
    } catch (DbExecuteQueryException | DbExecuteUpdateException | FileAlreadyExistsException e) {
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

      assertThrows(FileAlreadyExistsException.class, () -> fileMetadataRepository.addFile(file));
    } catch (DbExecuteUpdateException | DbExecuteQueryException | FileAlreadyExistsException e) {
      fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
    }
  }

    @Test
    void shouldAddAndDeleteFile_WithSameId() {
        FileEntity testFile = createTestFile();

        try {
            fileMetadataRepository.addFile(testFile);

            assertTrue(fileMetadataRepository.doesFileExist(testFile.getOwnerId(), testFile.getStoragePath()));

            fileMetadataRepository.deleteFile(testFile.getOwnerId(), testFile.getStoragePath());

            assertFalse(fileMetadataRepository.doesFileExist(testFile.getOwnerId(), testFile.getStoragePath()));

        } catch (DbExecuteQueryException | DbExecuteUpdateException | FileAlreadyExistsException |
                 FileNotFoundException e) {
            fail(UNEXPECTED_DB_EXCEPTION_MESSAGE, e);
        }
    }

  private static void addTestUser() {
    // TODO: добавить нормально, через интерфейс

    testUserUuid = UUID.randomUUID();

    try {
      postgresConnection.executeUpdate(
          "INSERT INTO users (id, email, password_hash, username, storage_limit, used_storage, is_active) "
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
