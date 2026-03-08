package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageJpaRepository extends JpaRepository<StorageEntity, UUID> {

  Optional<StorageEntity> findByIdAndStatus(UUID id, FileStatus status);

  Optional<StorageEntity> findByUserIdAndIdAndStatus(UUID userId, UUID id, FileStatus status);

  List<StorageEntity> findByStatusInAndUpdatedAtBefore(
      List<FileStatus> statuses, LocalDateTime threshold);

  @Query(
      """
        SELECT f FROM StorageEntity f
        WHERE f.userId = :userId
          AND f.name = :name
          AND f.status = 'READY'
          AND (f.parentId = :parentId OR (:parentId IS NULL AND f.parentId IS NULL))
    """)
  Optional<StorageEntity> findReadyFile(
      @Param("userId") UUID userId, @Param("parentId") UUID parentId, @Param("name") String name);

  @Query(
      """
        SELECT COUNT(f) > 0 FROM StorageEntity f
        WHERE f.userId = :userId
          AND f.name = :name
          AND (:onlyReady = false OR f.status = 'READY')
          AND (f.parentId = :parentId OR (:parentId IS NULL AND f.parentId IS NULL))
    """)
  boolean existsFile(
      @Param("userId") UUID userId,
      @Param("parentId") UUID parentId,
      @Param("name") String name,
      @Param("onlyReady") boolean onlyReady);

  @Modifying
  @Transactional
  void deleteByUserIdAndId(UUID userId, UUID id);

  @Query(
      "SELECT f FROM StorageEntity f WHERE f.userId = :userId "
          + "AND f.name = :name AND f.status = 'READY' "
          + "AND (f.parentId = :parentId OR (:parentId IS NULL AND f.parentId IS NULL))")
  Optional<StorageEntity> findFileInFolder(
      @Param("userId") UUID userId, @Param("parentId") UUID parentId, @Param("name") String name);

  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
        INSERT INTO files (id, user_id, parent_id, name, size, mime_type, visibility, is_deleted, tags, is_directory, status, operation_type, started_at, updated_at, retry_count, error_message)
        VALUES (:#{#f.id}, :#{#f.userId}, :#{#f.parentId}, :#{#f.name}, :#{#f.size}, :#{#f.mimeType}, :#{#f.visibility}, :#{#f.isDeleted}, :tagsStr, :#{#f.isDirectory}, :#{#f.status.name()}, :#{#f.operationType?.name()}, :#{#f.startedAt}, :#{#f.updatedAt}, :#{#f.retryCount}, :#{#f.errorMessage})
        ON CONFLICT (user_id, name, (COALESCE(parent_id, '00000000-0000-0000-0000-000000000000')))
        DO UPDATE SET
            size = EXCLUDED.size,
            mime_type = EXCLUDED.mime_type,
            tags = EXCLUDED.tags,
            status = 'PENDING',
            error_message = NULL,
            updated_at = NOW()
        WHERE files.status != 'READY'
    """)
  void upsertFile(@Param("f") StorageEntity file, @Param("tagsStr") String tagsStr);

  @Query(
      nativeQuery = true,
      value =
          """
        WITH RECURSIVE descendants AS (
            SELECT id FROM files WHERE id = :sourceId
            UNION ALL
            SELECT f.id FROM files f INNER JOIN descendants d ON f.parent_id = d.id
        )
        SELECT EXISTS (SELECT 1 FROM descendants WHERE id = :targetId)
    """)
  boolean isDescendant(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId);

  @Query(
      nativeQuery = true,
      value =
"""
    WITH RECURSIVE folder_tree AS (
        SELECT id, size FROM files WHERE id = :directoryId
        UNION ALL
        SELECT f.id, f.size FROM files f INNER JOIN folder_tree ft ON f.parent_id = ft.id
    )
    SELECT COALESCE(SUM(size), 0) FROM folder_tree
""")
  long calculateTotalSizeOfTree(@Param("directoryId") UUID directoryId);
}
