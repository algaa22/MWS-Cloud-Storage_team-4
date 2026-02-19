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
    if (fileExists(fileEntity.getUserId(), fileEntity.getPath(), false)) {
      throw new StorageFileAlreadyExistsException(fileEntity.getPath());
    }

    postgres.executeUpdate(
        "INSERT INTO files (id, user_id, path, size, mime_type, visibility, is_deleted, tags, is_directory, " +
            "status, operation_type, started_at, updated_at, retry_count, error_message) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
        Arrays.asList(
            fileEntity.getId(),
            fileEntity.getUserId(),
            fileEntity.getPath(),
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
            fileEntity.getErrorMessage()
        ));
  }

  public List<StorageEntity> getStaleFiles(LocalDateTime threshold) {
    return postgres.executeQuery(
        "SELECT * FROM files WHERE status in ('PENDING', 'ERROR') AND updated_at < ?",
        List.of(Timestamp.valueOf(threshold)),
        this::createStorageEntityByResultSet);
  }

  public List<StorageEntity> getFilesList(FileListFilter filter) {
    String query =
        "SELECT * FROM files WHERE user_id = ? AND path LIKE ? AND path != ? AND is_deleted = FALSE AND status = 'READY'";
    List<Object> params = new ArrayList<>();

    params.add(filter.userId());
    params.add(filter.searchDirectory() + "%");
    params.add(filter.searchDirectory());

    if (!filter.recursive()) {
      query += " AND PATH NOT LIKE ?";
      params.add(filter.searchDirectory() + "%/_%");
    }

    if (!filter.includeDirectories()) {
      query += " AND is_directory = FALSE";
    }

    return postgres.executeQuery(query, params, this::createStorageEntityByResultSet);
  }

  public Optional<StorageEntity> getFile(UUID fileId) {
    return getFile("SELECT * FROM files WHERE id = ? AND status = 'READY';", fileId);
  }

  public Optional<StorageEntity> getFile(UUID userId, String path) {
    return getFile(
        "SELECT * FROM files WHERE user_id = ? AND path = ? AND status = 'READY';", userId, path);
  }

  private Optional<StorageEntity> getFile(String sql, Object... params) {
    List<StorageEntity> result =
        postgres.executeQuery(sql, List.of(params), this::createStorageEntityByResultSet);

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(result.getFirst());
  }

  public void deleteFile(UUID userId, String path) throws StorageFileNotFoundException {
    if (!fileExists(userId, path, false)) {
      throw new StorageFileNotFoundException(path);
    }

    postgres.executeUpdate(
        "DELETE FROM files WHERE user_id = ? AND path = ?;", List.of(userId, path));
  }

  public void updateEntity(StorageEntity fileEntity) {
    postgres.executeUpdate(
        "UPDATE files SET path = ?, visibility = ?, tags = ?, size = ?, status = ?, "
            + "operation_type = ?, retry_count = ?, started_at = ?, updated_at = ?, error_message = ? "
            + "WHERE user_id = ? AND id = ?",
        Arrays.asList(
            fileEntity.getPath(),
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

  public boolean fileExists(UUID userId, String path) {
    return fileExists(userId, path, true);
  }

  public boolean fileExists(UUID userId, String path, boolean isOnlyReady) {
    String query = "SELECT EXISTS (SELECT 1 FROM files WHERE user_id = ? AND PATH = ?";

    if (isOnlyReady) {
      query += " AND status = 'READY'";
    }

    query += ");";

    List<Boolean> result =
        postgres.executeQuery(
            query,
            List.of(userId, path),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  private StorageEntity createStorageEntityByResultSet(ResultSet rs) throws SQLException {
    return StorageEntity.builder()
        .id(getUUID(rs, "id"))
        .userId(getUUID(rs, "user_id"))
        .path(rs.getString("path"))
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

  private <E extends Enum<E>> E getEnum(ResultSet rs, String column, Class<E> clazz) throws SQLException {
    String val = rs.getString(column);
    return val != null ? Enum.valueOf(clazz, val) : null;
  }
}
