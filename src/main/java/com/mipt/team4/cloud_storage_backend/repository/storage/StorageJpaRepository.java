package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.antivirus.model.enums.ScanVerdict;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageJpaRepository extends JpaRepository<StorageEntity, UUID> {
  @Query(
      """
    SELECT s FROM StorageEntity s
    WHERE s.userId = :userId
      AND s.name = :name
      AND (s.parentId = :parentId OR (:parentId IS NULL AND s.parentId IS NULL))
    """)
  Optional<StorageEntity> findByParentIdAndNameIncludeDeleted(
      @Param("userId") UUID userId, @Param("parentId") UUID parentId, @Param("name") String name);

  @Query(nativeQuery = true, value = "SELECT * FROM files WHERE id = :id AND user_id = :userId")
  Optional<StorageEntity> findByIdIncludeDeleted(
      @Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      nativeQuery = true,
      value =
          """
        SELECT * FROM files
        WHERE user_id = :userId
          AND is_deleted = true
        ORDER BY deleted_at DESC
    """)
  Page<StorageEntity> findAllDeletedByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query(
      """
        SELECT COUNT(f) > 0 FROM StorageEntity f
        WHERE f.userId = :userId
          AND f.name = :name
          AND (:onlyReady = false OR f.status = 'READY')
          AND (f.parentId = :parentId OR (:parentId IS NULL AND f.parentId IS NULL))
          AND f.isDeleted = false
    """)
  boolean existsFile(
      @Param("userId") UUID userId,
      @Param("parentId") UUID parentId,
      @Param("name") String name,
      @Param("onlyReady") boolean onlyReady);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE StorageEntity s SET s.isDeleted = true, s.deletedAt = CURRENT_TIMESTAMP "
          + "WHERE s.id = :id AND s.userId = :userId")
  void softDelete(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(flushAutomatically = true)
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
        SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT id FROM folder_tree)
    """)
  void softDeleteRecursive(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(flushAutomatically = true)
  @Query(
      nativeQuery = true,
      value =
          """
    UPDATE files
    SET is_deleted = false, deleted_at = NULL
    WHERE id = :id AND user_id = :userId
""")
  void restore(@Param("userId") UUID userId, @Param("id") UUID id);

  @Modifying(flushAutomatically = true)
  @Query(
      """
        UPDATE StorageEntity s
        SET s.status = :status,
            s.retryCount = :retryCount,
            s.updatedAt = :updatedAt,
            s.startedAt = :startedAt,
            s.operationType = :opType,
            s.errorMessage = :errorMessage
        WHERE s.id = :id
    """)
  void syncLifecycleMetadata(
      @Param("id") UUID id,
      @Param("status") FileStatus status,
      @Param("retryCount") int retryCount,
      @Param("opType") FileOperationType opType,
      @Param("startedAt") LocalDateTime startedAt,
      @Param("updatedAt") LocalDateTime updatedAt,
      @Param("errorMessage") String errorMessage);

  @Modifying(flushAutomatically = true)
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
        SET is_deleted = false, deleted_at = NULL
        WHERE id IN (SELECT id FROM folder_tree)
    """)
  void restoreRecursive(@Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      nativeQuery = true,
      value =
          "SELECT * FROM files WHERE id = CAST(:id AS uuid) AND user_id = CAST(:userId AS uuid) AND is_deleted = true")
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

  @Modifying(flushAutomatically = true)
  @Query(value = "UPDATE StorageEntity s SET status = :newStatus WHERE id=:id")
  void updateStatus(UUID id, FileStatus newStatus);

  @Modifying
  @Query("DELETE FROM StorageEntity f WHERE f.userId = :userId AND f.id = :id")
  int deleteByUserIdAndId(@Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      "SELECT f FROM StorageEntity f WHERE f.userId = :userId AND f.isDeleted = false ORDER BY f.updatedAt ASC")
  List<StorageEntity> findByUserIdAndIsDeletedFalseOrderByUpdatedAtAsc(
      @Param("userId") UUID userId, Pageable pageable);

  @Modifying
  @Query(value = "DELETE FROM files WHERE user_id = :userId AND id = :id", nativeQuery = true)
  int hardDeleteNative(@Param("userId") UUID userId, @Param("id") UUID id);

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
      "SELECT COALESCE(SUM(f.size), 0) FROM StorageEntity f WHERE f.userId = :userId AND f.isDeleted = false")
  Long sumFileSizesByUserId(@Param("userId") UUID userId);

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
                      SELECT * FROM files WHERE id = :id AND user_id = :userId AND is_directory = false
                      UNION ALL
                      SELECT f.* FROM files f INNER JOIN folder_tree ft ON f.parent_id = ft.id AND f.is_directory = false
                  )
                  SELECT * FROM folder_tree
              """)
  List<StorageEntity> findAllFilesDescendants(@Param("userId") UUID userId, @Param("id") UUID id);

  @Query(
      nativeQuery = true,
      value = "SELECT * FROM files WHERE is_deleted = true AND deleted_at < :threshold")
  Slice<StorageEntity> findStaleDeletedFiles(
      @Param("threshold") LocalDateTime threshold, Pageable pageable);

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

  @Query(
      value =
          """
        WITH RECURSIVE descendants AS (
            SELECT id, status, scan_verdict
            FROM storage
            WHERE parent_id = :parentId AND user_id = :userId

            UNION ALL

            SELECT s.id, s.status, s.scan_verdict
            FROM storage s
            INNER JOIN descendants d ON s.parent_id = d.id
            WHERE s.user_id = :userId
        )
        SELECT EXISTS (
            SELECT 1 FROM descendants
            WHERE status = :#{#status.name()}
               OR scan_verdict = :#{#verdict.name()}
        )
        """,
      nativeQuery = true)
  boolean existsLockedDescendants(
      @Param("userId") UUID userId,
      @Param("parentId") UUID parentId,
      @Param("status") FileStatus status,
      @Param("verdict") ScanVerdict verdict);

  @Query(
      "SELECT s FROM StorageEntity s WHERE s.userId = :userId AND s.id = :id AND s.isDeleted = false")
  Optional<StorageEntity> findByUserIdAndId(UUID userId, UUID id);

  @Query(
      "SELECT s FROM StorageEntity s WHERE s.userId = :userId AND s.parentId = :parentId AND s.name = :name AND s.isDeleted = false")
  Optional<StorageEntity> findByUserIdAndIdAndName(UUID userId, UUID parentId, String name);

  @Query("SELECT SUM(f.size) FROM FileEntity f WHERE f.userId = :userId")
  Long calculateTotalSizeByUserId(@Param("userId") UUID userId);

  Slice<StorageEntity> findByStatusInAndUpdatedAtBefore(
      List<FileStatus> statuses, LocalDateTime threshold, Pageable pageable);

  Slice<StorageEntity> findByStatusAndUpdatedAtBefore(
      FileStatus status, LocalDateTime threshold, Pageable pageable);

  Slice<StorageEntity> findByScanVerdictAndUpdatedAtBefore(
      ScanVerdict scanVerdict, LocalDateTime threshold, Pageable pageable);
}
