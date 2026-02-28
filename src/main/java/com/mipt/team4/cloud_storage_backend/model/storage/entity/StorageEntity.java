package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;

/**
 * Метаданные объекта хранения (файла или директории).
 *
 * <p>Является центральным узлом State Machine проекта. Состояние сущности управляется через {@code
 * StorageRepositoryWrapper}, который синхронизирует жизненный цикл физического файла в S3 и записи
 * в PostgreSQL.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageEntity {

  @EqualsAndHashCode.Include private final UUID id;

  private final UUID userId;
  private final String mimeType;
  private final boolean isDirectory;

  @Builder.Default private String visibility = FileVisibility.PRIVATE.toString();

  @Builder.Default private boolean isDeleted = false;

  private List<String> tags;
  private String path;
  private long size;

  /**
   * Текущий статус обработки. Блокирует файл для параллельных операций, если статус отличен от
   * {@code READY}.
   */
  @Builder.Default private FileStatus status = FileStatus.READY;

  /**
   * Счетчик ретраев для текущей операции. Используется {@code FileCleanupService} для
   * автоматического восстановления или перевода в {@code FATAL} статус.
   */
  @Builder.Default private int retryCount = 0;

  private FileOperationType operationType;
  private LocalDateTime startedAt;

  /**
   * Метка времени последнего изменения состояния сущности.
   *
   * <p>Выполняет две ключевые функции:
   *
   * <p>1. Служит сигналом "живучести" (Heartbeat) для {@code FileCleanupService}. Если файл долго
   * находится в PENDING или ERROR без обновления этой метки, он считается застрявшим.
   *
   * <p>2. Позволяет реализовать Throttled Update — пропуск избыточных записей в БД при потоковой
   * загрузке чанков, что снижает нагрузку на дисковую подсистему.
   */
  private LocalDateTime updatedAt;

  private String errorMessage;

  /** Генерирует уникальный детерминированный ключ для S3. */
  public String getS3Key() {
    return StoragePaths.getS3Key(userId, id);
  }
}
