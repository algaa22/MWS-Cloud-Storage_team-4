package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

  private static final long FREE_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024; // 5GB
  private final StorageJpaRepositoryAdapter storageJpaRepositoryAdapter;
  private final StorageRepository storageRepository;
  private final StorageRepositoryWrapper storageRepositoryWrapper;
  private final UserJpaRepositoryAdapter userRepository;
  private final FileErasureService erasureService;

  /**
   * Удаляет самые старые файлы пользователя до достижения размера sizeToDelete Используется при
   * просрочке подписки
   */
  @Transactional
  public void deleteOldestFiles(UUID userId, long sizeToDelete) {
    log.info("Deleting oldest files for user: {}, size to delete: {}", userId, sizeToDelete);

    List<StorageEntity> oldestFiles =
        storageJpaRepositoryAdapter.findOldestFilesByUserId(userId, PageRequest.of(0, 100));
    long deletedSize = 0;

    for (StorageEntity file : oldestFiles) {
      if (deletedSize >= sizeToDelete) break;

      deletedSize += file.getSize();

      try {
        // Используем erasureService для жесткого удаления
        erasureService.hardDelete(file);

        log.info(
            "Deleted file {} for user {} due to subscription expiration, size: {}",
            file.getId(),
            userId,
            file.getSize());
      } catch (Exception e) {
        log.error("Failed to delete file {} for user {}", file.getId(), userId, e);
      }
    }

    userRepository.decreaseUsedStorage(userId, deletedSize);
    log.info("Successfully deleted {} bytes for user {}", deletedSize, userId);
  }
}
