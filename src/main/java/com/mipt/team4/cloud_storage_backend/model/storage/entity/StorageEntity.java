package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsConverter;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Метаданные объекта хранения (файла или директории).
 *
 * <p>Является центральным узлом State Machine проекта. Состояние сущности управляется через {@code
 * StorageRepositoryWrapper}, который синхронизирует жизненный цикл физического файла в S3 и записи
 * в PostgreSQL.
 */
@Entity
@Table(name = "files")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "mime_type")
  private String mimeType;

  @Column(name = "is_directory")
  private boolean isDirectory;

  @Builder.Default
  @Column(name = "visibility")
  private String visibility = FileVisibility.PRIVATE.toString();

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted = false;

  @Convert(converter = FileTagsConverter.class)
  private List<String> tags;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(nullable = false)
  private String name;

  @Column(name = "size")
  private long size;

  /**
   * Текущий статус обработки. Блокирует файл для параллельных операций, если статус отличен от
   * {@code READY}.
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FileStatus status = FileStatus.READY;

  /**
   * Счетчик ретраев для текущей операции. Используется {@code FileCleanupService} для
   * автоматического восстановления или перевода в {@code FATAL} статус.
   */
  @Builder.Default
  @Column(name = "retry_count")
  private int retryCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type")
  private FileOperationType operationType;

  @Column(name = "started_at")
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
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  /** Генерирует уникальный детерминированный ключ для S3. */
  public String getS3Key() {
    return StoragePaths.getS3Key(userId, id);
  }
}
