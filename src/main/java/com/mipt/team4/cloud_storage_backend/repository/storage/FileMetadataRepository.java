package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
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

  public void addFile(StorageEntity fileEntity) throws StorageFileAlreadyExistsException {
    if (fileExists(fileEntity.getUserId(), fileEntity.getParentId(), fileEntity.getName(), false)) {
      throw new StorageFileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    postgres.executeUpdate(
        "INSERT INTO files (id, user_id, parent_id, name, size, mime_type, visibility, is_deleted, tags, is_directory, "
            + "status, operation_type, started_at, updated_at, retry_count, error_message) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
        Arrays.asList(
            fileEntity.getId(),
            fileEntity.getUserId(),
            fileEntity.getParentId(),
            fileEntity.getName(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.isDirectory(),
            fileEntity.getStatus() != null ? fileEntity.getStatus().name() : "PENDING",
            fileEntity.getOperationType() != null ? fileEntity.getOperationType().name() : null,
            fileEntity.getStartedAt(),
            fileEntity.getUpdatedAt(),
            fileEntity.getRetryCount(),
            fileEntity.getErrorMessage()));
  }

  public List<StorageEntity> getStaleFiles(LocalDateTime threshold) {
    return postgres.executeQuery(
        "SELECT * FROM files WHERE status in ('PENDING', 'ERROR') AND updated_at < ?",
        List.of(Timestamp.valueOf(threshold)),
        this::createStorageEntityByResultSet);
  }

  public List<StorageEntity> getFilesList(FileListFilter filter) {
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

  private Optional<StorageEntity> getFile(String sql, Object... params) {
    List<StorageEntity> result =
        postgres.executeQuery(sql, Arrays.asList(params), this::createStorageEntityByResultSet);

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(result.getFirst());
  }

  public Optional<StorageEntity> getFileById(UUID fileId) {
    String query = "SELECT * FROM files WHERE id = ?;";

    List<StorageEntity> result =
        postgres.executeQuery(query, List.of(fileId), this::createStorageEntityByResultSet);

    return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
  }

  public void deleteFile(UUID userId, UUID parentId, String name)
      throws StorageFileNotFoundException {
    if (!fileExists(userId, parentId, name, false)) {
      throw new StorageFileNotFoundException(parentId, name);
    }

    postgres.executeUpdate(
        "DELETE FROM files WHERE user_id = ? AND parent_id IS NOT DISTINCT FROM ? AND name = ?;",
        Arrays.asList(userId, parentId, name));
  }

  public void updateEntity(StorageEntity fileEntity) {
    postgres.executeUpdate(
        "UPDATE files SET parent_id = ?, name = ?, visibility = ?, tags = ?, size = ?, status = ?, "
            + "operation_type = ?, retry_count = ?, started_at = ?, updated_at = ?, error_message = ? "
            + "WHERE user_id = ? AND id = ?",
        Arrays.asList(
            fileEntity.getParentId(),
            fileEntity.getName(),
            fileEntity.getVisibility(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.getSize(),
            fileEntity.getStatus() != null ? fileEntity.getStatus().name() : null,
            fileEntity.getOperationType() != null ? fileEntity.getOperationType().name() : null,
            fileEntity.getRetryCount(),
            fileEntity.getStartedAt(),
            fileEntity.getUpdatedAt(),
            fileEntity.getErrorMessage(),
            fileEntity.getUserId(),
            fileEntity.getId()));
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

  private StorageEntity createStorageEntityByResultSet(ResultSet rs) throws SQLException {
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
        .tags(FileTagsMapper.toList(rs.getString("tags")))
        .status(getEnum(rs, "status", FileStatus.class))
        .operationType(getEnum(rs, "operation_type", FileOperationType.class))
        .startedAt(getLocalDateTime(rs, "started_at"))
        .updatedAt(getLocalDateTime(rs, "updated_at"))
        .retryCount(rs.getInt("retry_count"))
        .errorMessage(rs.getString("error_message"))
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
}
