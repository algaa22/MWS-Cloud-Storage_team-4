package com.mipt.team4.cloud_storage_backend.repository.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.mipt.team4.cloud_storage_backend.base.BasePostgresTest;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ChangeMetadataRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileLockedException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
class StorageRepositoryWrapperTest extends BasePostgresTest {
  @Autowired private StorageRepositoryWrapper wrapper;
  @Autowired private StorageJpaRepositoryAdapter metadataRepository;
  @Autowired private UserJpaRepositoryAdapter userRepository;
  @Autowired private StorageProps storageProps;
  @Autowired private EntityManager entityManager;

  private static final String LAMBDA_NOT_EXECUTED =
      "Wrapper should execute the lambda which return true";

  private UserEntity currentUser;

  @BeforeEach
  public void beforeEach() {
    currentUser = createTestUser(UUID.randomUUID());
  }

  @Nested
  class RecoverableErrorTests {
    @Test
    void whenRecoverableErrorAndTwoFailsafeRetries_completesReady() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      final int RETRY_COUNT = storageProps.failsafeRetry().maxAttempts() - 1;
      AtomicInteger retryCounter = new AtomicInteger(0);

      assertTrue(
          wrapper.wrapUpdate(
              file,
              operationType,
              () -> {
                if (retryCounter.getAndIncrement() < RETRY_COUNT) {
                  throw new RecoverableStorageException(null);
                }

                return true;
              }),
          LAMBDA_NOT_EXECUTED);

      assertEquals(RETRY_COUNT + 1, retryCounter.get());
      assertReady(file, operationType);
    }

    @Test
    void delete_whenRecoverableErrorAndMaxFailsafeRetries_increaseRetryCount() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.DELETE;

      assertRetryCountIncreased(file, operationType, 0);
    }

    @Test
    void
        upload_whenRecoverableErrorAndMaxFailsafeRetriesAndRetryCountIsPositive_increaseRetryCount() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      final int OLD_RETRY_COUNT = 1;
      file.setRetryCount(OLD_RETRY_COUNT);

      assertRetryCountIncreased(file, operationType, OLD_RETRY_COUNT);
    }

    @Test
    void
        upload_whenRecoverableErrorAndMaxFailsafeRetries_throwsRetriableExceptionAndRemainsPending() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      simulateMaxFailsafeRetriesError(
          file,
          operationType,
          UploadRetriableException.class,
          () -> {
            throw new RecoverableStorageException(null);
          });

      assertPending(file, operationType);
    }

    @Test
    void
        changeMetadata_withRecoverableErrorAndMaxFailsafeRetries_throwsRetriableExceptionAndRemainsReady() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.CHANGE_METADATA;

      simulateMaxFailsafeRetriesError(
          file,
          operationType,
          ChangeMetadataRetriableException.class,
          () -> {
            throw new RecoverableStorageException(null);
          });

      assertReady(file, operationType);
    }
  }

  @Nested
  class FatalErrorTests {
    @Test
    void delete_whenRecoverableErrorAndMaxAllRetries_setFatalStatusAndThrow() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.DELETE;

      final int MAX_RETRY_COUNT = storageProps.stateMachine().maxRetryCount();
      for (int i = 0; i < MAX_RETRY_COUNT; ++i) {
        simulateMaxFailsafeRetriesError(
            file,
            operationType,
            RecoverableStorageException.class,
            () -> {
              throw new RecoverableStorageException(null);
            });
        file.setRetryCount(i + 1);
      }

      simulateMaxFailsafeRetriesError(
          file,
          operationType,
          FatalStorageException.class,
          () -> {
            throw new RecoverableStorageException(null);
          });

      assertError(file, FileStatus.FATAL, operationType, MAX_RETRY_COUNT, true);
    }

    @Test
    void whenFatalError_setFatalStatus() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      simulateMaxFailsafeRetriesError(
          file,
          operationType,
          FatalStorageException.class,
          () -> {
            throw new FatalStorageException("", null);
          });

      assertError(file, FileStatus.FATAL, operationType, 0, true);
    }
  }

  @Nested
  class UpdateTests {
    @Test
    void wrapUpdate_statusIsReady_completesReady() {
      wrapUpdate_shouldCompletesReady_whenStatusIs(FileStatus.READY);
    }

    @Test
    void wrapUpdate_statusIsError_completesReady() {
      wrapUpdate_shouldCompletesReady_whenStatusIs(FileStatus.ERROR);
    }

    @Test
    void wrapUpdate_statusIsNotReady_throwsFileLocked() {
      StorageEntity file = createAndSaveTestFile(FileStatus.PENDING);
      FileOperationType operationType = FileOperationType.CHANGE_METADATA;

      assertThrows(
          StorageFileLockedException.class,
          () -> wrapper.wrapUpdate(file, operationType, () -> null));
    }

    @Test
    void wrapUpdate_whenDelete_remainsPending() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.DELETE;

      assertTrue(wrapper.wrapUpdate(file, operationType, () -> true), LAMBDA_NOT_EXECUTED);

      assertPending(file, operationType);
    }

    private void wrapUpdate_shouldCompletesReady_whenStatusIs(FileStatus status) {
      StorageEntity file = createAndSaveTestFile(status);
      FileOperationType operationType = FileOperationType.CHANGE_METADATA;

      assertTrue(
          wrapper.wrapUpdate(
              file,
              operationType,
              () -> {
                if (status == FileStatus.PENDING) {
                  assertPending(file, operationType);
                } else if (status == FileStatus.ERROR) {
                  assertError(file, status, operationType, 0, false);
                }

                return true;
              }),
          LAMBDA_NOT_EXECUTED);

      assertReady(file, operationType);
    }
  }

  @Nested
  class NewEntityTests {
    @Test
    void wrapNewEntityTask_completesReady() {
      StorageEntity file = createTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      assertTrue(
          wrapper.wrapNewEntityTask(
              file,
              operationType,
              () -> {
                assertPending(file, operationType);
                metadataRepository.addFile(file);
                return true;
              }),
          LAMBDA_NOT_EXECUTED);

      assertReady(file, operationType);
    }

    @Test
    void initiateNewFileStep_executeLambda() {
      StorageEntity file = createTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      assertTrue(
          wrapper.initiateNewFileStep(
              file,
              operationType,
              () -> {
                assertPending(file, operationType);
                return true;
              }),
          LAMBDA_NOT_EXECUTED);
    }
  }

  @Nested
  class ProcessStepTests {
    @Test
    void processStep_statusIsPending_updatedAtChanged() {
      StorageEntity file = createTestFile(FileStatus.PENDING);
      FileOperationType operationType = FileOperationType.UPLOAD;

      LocalDateTime oldUpdatedAt = file.getUpdatedAt().minusDays(1);
      file.setUpdatedAt(oldUpdatedAt);

      metadataRepository.addFile(file);
      performStepAndAssertChanged(file, operationType, oldUpdatedAt);
    }

    @Test
    void multipleProcessStep_statusIsPending_throttledChangeUpdatedAt() {
      StorageEntity file = createAndSaveTestFile(FileStatus.PENDING);
      FileOperationType operationType = FileOperationType.UPLOAD;

      LocalDateTime firstUpdatedAt = file.getUpdatedAt().minusDays(1);
      file.setUpdatedAt(firstUpdatedAt);

      performStepAndAssertChanged(file, operationType, firstUpdatedAt);

      LocalDateTime currentUpdatedAt = getCurrentUpdatedAt(file);
      file.setUpdatedAt(
          currentUpdatedAt.minusSeconds(
              storageProps.stateMachine().fileThrottledUpdateIntervalSec() / 2));

      performStepAndAssertNotChanged(file, operationType, currentUpdatedAt);

      file.setUpdatedAt(
          currentUpdatedAt.minusSeconds(
              storageProps.stateMachine().fileThrottledUpdateIntervalSec() + 1));

      performStepAndAssertChanged(file, operationType, currentUpdatedAt);
    }

    @Test
    void processStep_statusIsNotPending_throwsFileLocked() {
      StorageEntity file = createTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      assertThrows(
          StorageFileLockedException.class,
          () -> wrapper.processStep(file, operationType, () -> null));
    }
  }

  @Nested
  class CompletionAndResetTests {
    @Test
    void completeStep_statusIsPending_completesReady() {
      StorageEntity file = createAndSaveTestFile(FileStatus.PENDING);
      FileOperationType operationType = FileOperationType.UPLOAD;

      assertTrue(wrapper.completeStep(file, operationType, () -> true), LAMBDA_NOT_EXECUTED);

      assertReady(file, operationType);
    }

    @Test
    void completeStep_statusIsNotPending_throwsFileLocked() {
      StorageEntity file = createAndSaveTestFile(FileStatus.READY);
      FileOperationType operationType = FileOperationType.UPLOAD;

      assertThrows(
          StorageFileLockedException.class,
          () -> wrapper.completeStep(file, operationType, () -> null));
    }

    @Test
    void resetToReady_makeReady() {
      StorageEntity file = createAndSaveTestFile(FileStatus.PENDING);
      FileOperationType operationType = FileOperationType.CHANGE_METADATA;
      file.setRetryCount(2);

      wrapper.resetToReady(file, operationType);

      assertReady(file, operationType);
    }
  }

  private void simulateMaxFailsafeRetriesError(
      StorageEntity file,
      FileOperationType operationType,
      Class<? extends Exception> expectedError,
      ThrowingFunction throwingFunction) {
    assertThrows(
        expectedError,
        () ->
            wrapper.wrapUpdate(
                file,
                operationType,
                () -> {
                  throwingFunction.execute();
                  return null;
                }));
  }

  private void assertRetryCountIncreased(
      StorageEntity file, FileOperationType operationType, int oldRetryCount) {
    simulateMaxFailsafeRetriesError(
        file,
        operationType,
        RecoverableStorageException.class,
        () -> {
          throw new RecoverableStorageException(null);
        });

    assertError(file, FileStatus.ERROR, operationType, oldRetryCount + 1, true);
  }

  private void performStepAndAssertChanged(
      StorageEntity file, FileOperationType operationType, LocalDateTime oldUpdatedAt) {
    assertTrue(wrapper.processStep(file, operationType, () -> true), LAMBDA_NOT_EXECUTED);
    assertUpdatedAtChanged(file, oldUpdatedAt);
  }

  private void performStepAndAssertNotChanged(
      StorageEntity file, FileOperationType operationType, LocalDateTime oldUpdatedAt) {
    assertTrue(wrapper.processStep(file, operationType, () -> true), LAMBDA_NOT_EXECUTED);
    assertUpdatedAtNotChanged(file, oldUpdatedAt);
  }

  private LocalDateTime getCurrentUpdatedAt(StorageEntity testFile) {
    StorageEntity fileInDb = findEntityInDatabase(testFile.getId(), testFile.getUserId());
    return fileInDb.getUpdatedAt();
  }

  private void assertUpdatedAtChanged(StorageEntity testFile, LocalDateTime oldUpdatedAt) {
    StorageEntity fileInDb = findEntityInDatabase(testFile.getId(), testFile.getUserId());
    assertTrue(oldUpdatedAt.isBefore(fileInDb.getUpdatedAt()));
  }

  private void assertUpdatedAtNotChanged(StorageEntity testFile, LocalDateTime oldUpdatedAt) {
    StorageEntity fileInDb = findEntityInDatabase(testFile.getId(), testFile.getUserId());
    assertTrue(oldUpdatedAt.isEqual(fileInDb.getUpdatedAt()));
  }

  private void assertPending(StorageEntity testFile, FileOperationType operationType) {
    StorageEntity fileInDb =
        assertLifecycleMetadataIs(testFile, FileStatus.PENDING, operationType, 0);

    assertNotNull(fileInDb.getStartedAt());
  }

  private void assertReady(StorageEntity testFile, FileOperationType operationType) {
    StorageEntity fileInDb =
        assertLifecycleMetadataIs(testFile, FileStatus.READY, operationType, 0);

    assertNotNull(fileInDb.getUpdatedAt());
  }

  private void assertError(
      StorageEntity testFile,
      FileStatus expectedStatus,
      FileOperationType operationType,
      int expectedRetryCount,
      boolean shouldHasErrorMessage) {
    StorageEntity fileInDb =
        assertLifecycleMetadataIs(testFile, expectedStatus, operationType, expectedRetryCount);

    if (shouldHasErrorMessage) {
      assertNotNull(fileInDb.getErrorMessage());
    }
  }

  private StorageEntity assertLifecycleMetadataIs(
      StorageEntity testFile, FileStatus status, FileOperationType operationType, int retryCount) {
    StorageEntity fileInDb = findEntityInDatabase(testFile.getId(), testFile.getUserId());

    if (fileInDb == null) {
      fileInDb = testFile;
    }

    assertEquals(status, fileInDb.getStatus());
    assertEquals(operationType, fileInDb.getOperationType());
    assertEquals(retryCount, fileInDb.getRetryCount());

    return fileInDb;
  }

  private StorageEntity findEntityInDatabase(UUID id, UUID userId) {
    entityManager.flush();
    entityManager.clear();

    return metadataRepository.get(userId, id).orElse(null);
  }

  private StorageEntity createAndSaveTestFile(FileStatus status) {
    StorageEntity newFile = createTestFile(status);
    metadataRepository.addFile(newFile);
    return newFile;
  }

  private StorageEntity createTestFile(FileStatus status) {
    return StorageEntity.builder()
        .id(UUID.randomUUID())
        .status(status)
        .retryCount(0)
        .userId(currentUser.getId())
        .mimeType("application/octet-stream")
        .isDirectory(false)
        .visibility(FileVisibility.PRIVATE.name())
        .name("file")
        .updatedAt(LocalDateTime.now())
        .size(100)
        .build();
  }

  private UserEntity createTestUser(UUID id) {
    UserEntity newUser =
        UserEntity.builder()
            .username("user")
            .email("test@gmail.com")
            .passwordHash("hash")
            .storageLimit(100)
            .tariffPlan(TariffPlan.TRIAL)
            .build();

    userRepository.addUser(newUser);

    return newUser;
  }

  @FunctionalInterface
  private interface ThrowingFunction {
    void execute() throws BaseStorageException;
  }
}
