package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис автоматической очистки и восстановления консистентности хранилища.
 *
 * <p>Выполняет периодическую проверку объектов, которые находятся в переходных состояниях (PENDING)
 * дольше допустимого времени. Предотвращает утечки места в S3 и "зависание" метаданных в БД
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {
  private final StorageRepositoryWrapper storageRepositoryWrapper;
  private final FileMetadataRepository metadataRepository;
  private final StorageRepository storageRepository;
  private final StorageConfig storageConfig;

  /**
   * Периодическая задача по поиску и обработке "протухших" (stale) файлов.
   *
   * <p>Выполняется по расписанию (cron). Файл считается stale, если его последнее обновление
   * ({@code updatedAt}) было произведено ранее, чем {@code now() - staleTimeThreshold}.
   */
  @Scheduled(cron = "0 0 * * * *")
  public void cleanupStaleFiles() {
    int staleTime = storageConfig.stateMachine().fileStaleTimeMin();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTime);
    List<StorageEntity> staleFiles = metadataRepository.getStaleFiles(threshold);

    if (staleFiles.isEmpty()) {
      return;
    }

    log.info("Found {} stale files", staleFiles.size());

    for (StorageEntity entity : staleFiles) {
      try {
        handleStaleFile(entity);
      } catch (Exception e) {
        log.error("Failed to cleanup file {}", entity.getId(), e);
      }
    }
  }

  /**
   * Применяет стратегию восстановления в зависимости от типа прерванной операции:
   *
   * <ul>
   *   <li><b>UPLOAD:</b> Полное удаление объекта из S3 и метаданных из БД, так как целостность
   *       файла не гарантирована (загрузка прервана).
   *   <li><b>DELETE:</b> Повторная попытка удаления. Если файл "завис" в статусе удаления, значит
   *       физический объект в S3 мог остаться.
   *   <li><b>CHANGE_METADATA:</b> Принудительный откат (Rollback) к предыдущему валидному состоянию
   *       метаданных.
   * </ul>
   *
   * @param entity сущность, требующая очистки или восстановления.
   */
  // TODO: @Transactional
  private void handleStaleFile(StorageEntity entity) {
    switch (entity.getOperationType()) {
      case UPLOAD -> {
        storageRepository.deleteFile(entity);
        log.info("Cleanup: Deleted stale upload for file {}", entity.getId());
      }
      case DELETE -> {
        storageRepository.deleteFile(entity);
        log.info("Cleanup: Retried deletion for file {}", entity.getId());
      }
      case CHANGE_METADATA -> {
        storageRepositoryWrapper.resetToReady(entity);
        log.info(
            "Cleanup: Forced rollback to stuck metadata operation for file {}", entity.getId());
      }
      default ->
          throw new IllegalStateException(
              "FATAL: Unknown operation type for file " + entity.getId());
    }
  }
}
