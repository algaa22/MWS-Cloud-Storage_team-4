package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
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
  @Query(
      "SELECT s FROM StorageEntity s WHERE s.userId = :userId AND s.name = :name AND s.parentId = :parentId")
  Optional<StorageEntity> findByParentIdAndNameIncludeDeleted(
      @Param("userId") UUID userId, @Param("parentId") UUID parentId, @Param("name") String name);

  @Query(nativeQuery = true, value = "SELECT * FROM files WHERE id = :id AND user_id = :userId")
  Optional<StorageEntity> findByIdIncludeDeleted(
      @Param("userId") UUID userId, @Param("id") UUID id);

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

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE StorageEntity s SET s.isDeleted = true, s.deletedAt = CURRENT_TIMESTAMP, s.updatedAt = CURRENT_TIMESTAMP "
          + "WHERE s.id = :id AND s.userId = :userId")
  void softDelete(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      nativeQuery = true,
      value =
          """
        WITH RECURSIVE folder_tree AS (
            SELECT id FROM files WHERE id = :id AND user_id = :userId
            UNION ALL
            SELECT f.id FROM files f INNER JOIN folder_tree ft ON f.parent_id = ft.id
        )
        UPDATE files
        SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT id FROM folder_tree)
    """)
  void softDeleteRecursive(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE StorageEntity s SET s.isDeleted = false, s.deletedAt = NULL, s.updatedAt = CURRENT_TIMESTAMP "
          + "WHERE s.id = :id AND s.userId = :userId")
  void restore(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      nativeQuery = true,
      value =
          """
        WITH RECURSIVE folder_tree AS (
            SELECT id FROM files WHERE id = :id AND user_id = :userId
            UNION ALL
            SELECT f.id FROM files f INNER JOIN folder_tree ft ON f.parent_id = ft.id
        )
        UPDATE files
        SET is_deleted = false, deleted_at = NULL, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT id FROM folder_tree)
    """)
  void restoreRecursive(@Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      nativeQuery = true,
      value =
          """
    SELECT * FROM files
    WHERE user_id = :userId
      AND parent_id IS NOT DISTINCT FROM CAST(:parentId AS UUID)
      AND is_deleted = true
    ORDER BY is_directory DESC, name ASC
""")
  List<StorageEntity> findTrashByParentId(
      @Param("userId") UUID userId, @Param("parentId") UUID parentId);

  @Query(
      nativeQuery = true,
      value = "SELECT * FROM files WHERE id = :id AND user_id = :userId AND is_deleted = true")
  Optional<StorageEntity> findDeletedById(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(flushAutomatically = true)
  @Query(
      nativeQuery = true,
      value =
          """
                  INSERT INTO files (id, user_id, parent_id, name, size, mime_type, visibility, is_deleted, is_directory, status, operation_type, started_at, updated_at, retry_count, error_message)
                          VALUES (:#{#f.id}, :#{#f.userId}, :#{#f.parentId}, :#{#f.name}, :#{#f.size}, :#{#f.mimeType}, :#{#f.visibility}, :#{#f.isDeleted}, :#{#f.isDirectory}, :#{#f.status.name()}, :#{#f.operationType?.name()}, :#{#f.startedAt}, :#{#f.updatedAt}, :#{#f.retryCount}, :#{#f.errorMessage})
                          ON CONFLICT (user_id, name, (COALESCE(parent_id, '00000000-0000-0000-0000-000000000000')))
                          WHERE is_deleted = false

                          DO UPDATE SET
                              size = EXCLUDED.size,
                              mime_type = EXCLUDED.mime_type,
                              status = 'PENDING',
                              error_message = NULL,
                              updated_at = NOW()
                          WHERE files.status != 'READY'
              """)
  void upsertFile(@Param("f") StorageEntity file);

  @Modifying
  void deleteByUserIdAndId(UUID userId, UUID id);

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

  @Query(
      nativeQuery = true,
      value =
          """
    WITH RECURSIVE folder_tree AS (
        SELECT * FROM files WHERE id = :id AND user_id = :userId
        UNION ALL
        SELECT f.* FROM files f INNER JOIN folder_tree ft ON f.parent_id = ft.id
    )
    SELECT * FROM folder_tree WHERE is_directory = false
""")
  List<StorageEntity> findAllFilesDescendants(@Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      nativeQuery = true,
      value = "SELECT * FROM files WHERE is_deleted = true AND deleted_at < :threshold")
  List<StorageEntity> findStaleDeletedFiles(@Param("threshold") LocalDateTime threshold);

  @Query(
      value =
          """
      WITH RECURSIVE file_path AS (
          SELECT id, parent_id, name, 1 as level
          FROM files
          WHERE id = :fileId AND is_deleted = false

          UNION ALL

          SELECT f.id, f.parent_id, f.name, fp.level + 1
          FROM files f
          JOIN file_path fp ON f.id = fp.parent_id
          WHERE f.is_deleted = false
      )
      SELECT name FROM file_path ORDER BY level DESC
      """,
      nativeQuery = true)
  List<String> getFullPathNodes(@Param("fileId") UUID fileId);

  Optional<StorageEntity> findByUserIdAndId(UUID userId, UUID fileId);

  List<StorageEntity> findByStatusInAndUpdatedAtBefore(
      List<FileStatus> statuses, LocalDateTime threshold);

  Optional<StorageEntity> findByUserIdAndIdAndName(UUID userId, UUID parentId, String name);
}
