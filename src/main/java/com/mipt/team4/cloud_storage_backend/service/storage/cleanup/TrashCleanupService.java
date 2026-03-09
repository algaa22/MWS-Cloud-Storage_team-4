package com.mipt.team4.cloud_storage_backend.service.storage.cleanup;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.storage.FileErasureService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис автоматической окончательной очистки корзины (Trash Bin). *
 *
 * <p>Данный компонент реализует политику удержания данных (Retention Policy), отвечая за
 * безвозвратное удаление объектов, которые были помечены пользователем как удаленные (Soft Delete)
 * и превысили допустимый срок хранения в корзине. *
 *
 * <p>Процесс включает в себя:
 *
 * <ul>
 *   <li>Поиск метаданных файлов с флагом {@code isDeleted = true}.
 *   <li>Проверку времени удаления по полю {@code deletedAt}.
 *   <li>Делегирование физического удаления и пересчета квот сервису {@link FileErasureService}.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrashCleanupService {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final FileErasureService erasureService;
  private final StorageConfig storageConfig;

  /**
   * Очистка файлов, которые были удалены пользователем (Soft Delete) более N дней назад.
   * Выполняется раз в сутки (в 2 часа ночи).
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void cleanupTrash() {
    int daysToKeep = storageConfig.trash().retentionDays();
    LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
    List<StorageEntity> staleDeletedFiles = metadataRepository.getStaleDeletedFiles(threshold);

    if (staleDeletedFiles.isEmpty()) {
      return;
    }

    log.info("Trash Cleanup: Found {} items to be permanently removed", staleDeletedFiles.size());

    for (StorageEntity entity : staleDeletedFiles) {
      try {
        hardDelete(entity);
      } catch (Exception e) {
        log.error("Trash Cleanup: Failed to delete file {}", entity.getId(), e);
      }
    }
  }

  private void hardDelete(StorageEntity entity) {
    erasureService.hardDelete(entity);

    log.info(
        "Trash Cleanup: Permanently deleted file {} (owner: {})",
        entity.getId(),
        entity.getUserId());
  }
}
