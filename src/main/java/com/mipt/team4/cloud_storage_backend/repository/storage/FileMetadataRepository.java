package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FileMetadataRepository {
  private final PostgresConnection postgres;

  public void addFile(StorageEntity fileEntity) {
    postgres.executeUpdate(
        "INSERT INTO files (id, user_id, parent_id, name, size, mime_type, visibility, is_deleted, is_directory, "
            + "status, operation_type, started_at, updated_at, retry_count, error_message) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (user_id, name, (COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'))) "
            + "DO UPDATE SET "
            + "    size = EXCLUDED.size,"
            + "    mime_type = EXCLUDED.mime_type,"
            + "    status = 'PENDING',"
            + "    error_message = NULL,"
            + "    updated_at = NOW() "
            + "WHERE files.status != 'READY'",
        Arrays.asList(
            fileEntity.getId(),
            fileEntity.getUserId(),
            fileEntity.getParentId(),
            fileEntity.getName(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            fileEntity.isDirectory(),
            fileEntity.getStatus() != null ? fileEntity.getStatus().name() : "PENDING",
            fileEntity.getOperationType() != null ? fileEntity.getOperationType().name() : null,
            fileEntity.getStartedAt(),
            fileEntity.getUpdatedAt(),
            fileEntity.getRetryCount(),
            fileEntity.getErrorMessage()));
    for (String tag : fileEntity.getTags()) {
      postgres.executeUpdate(
          "INSERT INTO file_tags(file_id, tag) VALUES (?, ?) ON CONFLICT DO NOTHING",
          List.of(fileEntity.getId(), tag));
    }
  }

  public List<StorageEntity> getStaleFiles(LocalDateTime threshold) {
    return postgres.executeQuery(
        "SELECT * FROM files WHERE status in ('PENDING', 'ERROR') AND updated_at < ?",
        List.of(Timestamp.valueOf(threshold)),
        this::createStorageEntityByResultSet);
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    List<Object> params = new ArrayList<>();
    String query;

    if (filter.recursive()) {
      query =
          """
            WITH RECURSIVE folder_tree AS (
                SELECT * FROM files
                WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND is_deleted = FALSE

                UNION ALL

                SELECT f.* FROM files f
                INNER JOIN folder_tree ft ON f.parent_id = ft.id
                WHERE f.is_deleted = FALSE
            )
            SELECT * FROM folder_tree WHERE 1=1
        """;
    } else {
      query =
          "SELECT * FROM files WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND is_deleted = FALSE";
    }

    params.add(filter.userId());
    params.add(filter.parentId());

    if (!filter.includeDirectories()) {
      query += " AND is_directory = FALSE";
    }

    if (!filter.recursive()) {
      query += " AND status = 'READY'";
    }

    query += " ORDER BY CASE WHEN is_directory THEN 1 ELSE 2 END, name ASC";

    return postgres.executeQuery(query, params, this::createStorageEntityByResultSet);
  }

  public Optional<StorageEntity> getFile(UUID fileId) {
    return getFile("SELECT * FROM files WHERE id = ? AND status = 'READY';", fileId);
  }

  public Optional<StorageEntity> getFile(UUID userId, UUID parentId, String name) {
    return getFile(
        "SELECT * FROM files WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND name = ? AND status = 'READY';",
        userId,
        parentId,
        name);
  }

  public Optional<StorageEntity> getFile(UUID userId, UUID fileId) {
    return getFile(
        "SELECT * FROM files WHERE user_id = ? AND id = ? AND status = 'READY';", userId, fileId);
  }

  private Optional<StorageEntity> getFile(String sql, Object... params) {
    List<StorageEntity> result =
        postgres.executeQuery(sql, Arrays.asList(params), this::createStorageEntityByResultSet);

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(result.getFirst());
  }

  public void deleteFile(StorageEntity entity) {
    postgres.executeUpdate(
        "DELETE FROM files WHERE user_id = ? AND id = ?;",
        Arrays.asList(entity.getUserId(), entity.getId()));
    postgres.executeUpdate(
        "DELETE FROM file_tags WHERE file_id = ?", List.of(entity.getId()));
  }

  public void updateEntity(StorageEntity entity) {
    postgres.executeUpdate(
        "UPDATE files SET parent_id = ?, name = ?, visibility = ?, size = ?, status = ?, "
            + "operation_type = ?, retry_count = ?, started_at = ?, updated_at = ?, error_message = ? "
            + "WHERE user_id = ? AND id = ?",
        Arrays.asList(
            entity.getParentId(),
            entity.getName(),
            entity.getVisibility(),
            entity.getSize(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getOperationType() != null ? entity.getOperationType().name() : null,
            entity.getRetryCount(),
            entity.getStartedAt(),
            entity.getUpdatedAt(),
            entity.getErrorMessage(),
            entity.getUserId(),
            entity.getId()));
    for (String tag : entity.getTags()) {
      postgres.executeUpdate(
          "INSERT INTO file_tags(file_id, tag) VALUES (?, ?) ON CONFLICT DO NOTHING",
          List.of(entity.getId(), tag));
    }
  }

  public boolean fileExists(UUID userId, UUID parentId, String name) {
    return fileExists(userId, parentId, name, true);
  }

  public boolean fileExists(UUID userId, UUID parentId, String name, boolean isOnlyReady) {
    String query =
        "SELECT EXISTS (SELECT 1 FROM files WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND name = ?";

    if (isOnlyReady) {
      query += " AND status = 'READY'";
    }

    query += ");";

    List<Boolean> result =
        postgres.executeQuery(
            query, Arrays.asList(userId, parentId, name), rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  public boolean isDescendant(UUID sourceId, UUID targetParentId) {
    String query =
        """
        WITH RECURSIVE descendants AS (
            SELECT id FROM files WHERE id = ?
            UNION ALL
            SELECT f.id FROM files f
            INNER JOIN descendants d ON f.parent_id = d.id
        )
        SELECT EXISTS (SELECT 1 FROM descendants WHERE id = ?);
    """;

    List<Boolean> result =
        postgres.executeQuery(query, List.of(sourceId, targetParentId), rs -> rs.getBoolean(1));

    return !result.isEmpty() && result.getFirst();
  }

  public long calculateTotalSizeOfTree(UUID directoryId) {
    String sql =
        """
        WITH RECURSIVE folder_tree AS (
            SELECT id, size FROM files WHERE id = ?
            UNION ALL
            SELECT f.id, f.size FROM files f
            INNER JOIN folder_tree ft ON f.parent_id = ft.id
        )
        SELECT COALESCE(SUM(size), 0) FROM folder_tree;
    """;
    List<Long> result = postgres.executeQuery(sql, List.of(directoryId), rs -> rs.getLong(1));
    return result.isEmpty() ? 0L : result.getFirst();
  }

  public List<StorageEntity> getFilesByTags(UUID userId, List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return new ArrayList<>();
    }

    String sql =
        """
        SELECT f.*
        FROM files f
        JOIN file_tags ft ON f.id = ft.file_id
        WHERE f.owner_id = ?
          AND f.is_deleted = FALSE
          AND ft.tag = ANY (?)
        GROUP BY f.id
        HAVING COUNT(DISTINCT ft.tag) = ?
    """;

    return postgres.executeQuery(
        sql,
        List.of(userId, tags.toArray(new String[0]), tags.size()),
        this::createStorageEntityByResultSet);
  }

  private StorageEntity createStorageEntityByResultSet(ResultSet rs) throws SQLException {
    UUID fileId = UUID.fromString(rs.getString("id"));
    List<String> tags = getFileTags(fileId);
    return StorageEntity.builder()
        .id(getUUID(rs, "id"))
        .userId(getUUID(rs, "user_id"))
        .parentId(getUUID(rs, "parent_id"))
        .name(rs.getString("name"))
        .size(rs.getLong("size"))
        .mimeType(rs.getString("mime_type"))
        .visibility(rs.getString("visibility"))
        .isDeleted(rs.getBoolean("is_deleted"))
        .isDirectory(rs.getBoolean("is_directory"))
        .status(getEnum(rs, "status", FileStatus.class))
        .operationType(getEnum(rs, "operation_type", FileOperationType.class))
        .startedAt(getLocalDateTime(rs, "started_at"))
        .updatedAt(getLocalDateTime(rs, "updated_at"))
        .retryCount(rs.getInt("retry_count"))
        .errorMessage(rs.getString("error_message"))
        .tags(tags)
        .build();
  }

  private UUID getUUID(ResultSet rs, String column) throws SQLException {
    String val = rs.getString(column);
    return val != null ? UUID.fromString(val) : null;
  }

  private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return ts != null ? ts.toLocalDateTime() : null;
  }

  private <E extends Enum<E>> E getEnum(ResultSet rs, String column, Class<E> clazz)
      throws SQLException {
    String val = rs.getString(column);
    return val != null ? Enum.valueOf(clazz, val) : null;
  }

  private List<String> getFileTags(UUID fileId) {
    return postgres.executeQuery(
        "SELECT tag FROM file_tags WHERE file_id = ?",
        List.of(fileId),
        rs -> rs.getString("tag")
    );
  }

  public String getFullFilePath(UUID fileId) {
    String sql = """
        WITH RECURSIVE file_path AS (
            SELECT
                id,
                name,
                parent_id,
                name as full_path,
                1 as level
            FROM files
            WHERE id = ? AND is_deleted = false
        
            UNION ALL
        
            SELECT
                f.id,
                f.name,
                f.parent_id,
                f.name || '/' || fp.full_path,
                fp.level + 1
            FROM files f
            INNER JOIN file_path fp ON f.id = fp.parent_id
            WHERE f.is_deleted = false AND f.is_directory = true
        )
        SELECT full_path
        FROM file_path
        WHERE parent_id IS NULL  -- Дошли до корня
        ORDER BY level DESC
        LIMIT 1
        """;

    List<String> result = postgres.executeQuery(
        sql,
        List.of(fileId),
        rs -> rs.getString("full_path")
    );

    return result.isEmpty() ? null : result.getFirst();
  }

  public String getFileName(UUID fileId) {
    String sql = "SELECT name FROM files WHERE id = ?";
    List<String> result = postgres.executeQuery(
        sql,
        List.of(fileId),
        rs -> rs.getString("name")
    );
    return result.isEmpty() ? null : result.getFirst();
  }


}
