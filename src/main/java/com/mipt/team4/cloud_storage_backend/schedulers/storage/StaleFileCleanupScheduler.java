package com.mipt.team4.cloud_storage_backend.schedulers.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;
import com.mipt.team4.cloud_storage_backend.service.storage.FileErasureService;
import com.mipt.team4.cloud_storage_backend.utils.wrapper.BatchProcessor;
import java.time.LocalDateTime;
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
public class StaleFileCleanupScheduler {
  private final StorageRepositoryWrapper storageRepositoryWrapper;
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final FileErasureService erasureService;
  private final StorageConfig storageConfig;
  private final BatchProcessor batchProcessor;

  private static final String TASK_NAME = "Stale File Cleanup";

  /**
   * Периодическая задача по поиску и обработке "протухших" (stale) файлов.
   *
   * <p>Выполняется раз в сутки (в 1 час ночи). Файл считается stale, если его последнее обновление
   * ({@code updatedAt}) было произведено ранее, чем {@code now() - staleTimeThreshold}.
   */
  @Scheduled(cron = "${storage.scheduling.cron.stale-file-cleanup}")
  public void cleanupStaleFiles() {
    int staleTime = storageConfig.scheduling().staleTimeMin().file();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTime);

    batchProcessor.scoop(
        TASK_NAME,
        pageable -> metadataRepository.getStaleFiles(threshold, pageable),
        StorageEntity::getId,
        this::handleStaleFile);
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
  private void handleStaleFile(StorageEntity entity) {
    switch (entity.getOperationType()) {
      case UPLOAD -> {
        erasureService.hardDelete(entity);
        log.info("[{}] Deleted stale upload for file {}", TASK_NAME, entity.getId());
      }
      case DELETE -> {
        erasureService.hardDelete(entity);
        log.info("[{}] Retried deletion for file {}", TASK_NAME, entity.getId());
      }
      case CHANGE_METADATA -> {
        storageRepositoryWrapper.resetToReady(entity);
        log.info(
            "[{}] Forced rollback to stuck metadata operation for file {}",
            TASK_NAME,
            entity.getId());
      }
      default ->
          throw new IllegalStateException(
              "FATAL: Unknown operation type for file " + entity.getId());
    }
  }
}
