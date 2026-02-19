package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    if (fileExists(fileEntity.getUserId(), fileEntity.getPath())) {
      throw new StorageFileAlreadyExistsException(fileEntity.getPath());
    }

    postgres.executeUpdate(
        "INSERT INTO files (id, user_id, path, file_size, mime_type, visibility, is_deleted, tags, is_directory)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?);",
        List.of(
            fileEntity.getId(),
            fileEntity.getUserId(),
            fileEntity.getPath(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.isDirectory()));
  }

  public List<StorageEntity> getFilesList(FileListFilter filter) {
    String query =
        "SELECT * FROM files WHERE user_id = ? AND path LIKE ? AND path != ? AND is_deleted = FALSE";
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
    return getFile("SELECT * FROM files WHERE fileId = ?;", fileId);
  }

  public Optional<StorageEntity> getFile(UUID userId, String path) {
    return getFile("SELECT * FROM files WHERE user_id = ? AND path = ?;", userId, path);
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
    if (!fileExists(userId, path)) {
      throw new StorageFileNotFoundException(path);
    }

    postgres.executeUpdate(
        "DELETE FROM files WHERE user_id = ? AND path = ?;", List.of(userId, path));
  }

  public void updateEntity(StorageEntity fileEntity) {
    postgres.executeUpdate(
        "UPDATE files SET path = ?, visibility = ?, tags = ? WHERE user_id = ? AND id = ?",
        List.of(
            fileEntity.getPath(),
            fileEntity.getVisibility(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.getuserId(),
            fileEntity.getId()));
  }

  public boolean fileExists(UUID userId, String path) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE user_id = ? AND path = ?);",
            List.of(userId, path),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  private StorageEntity createStorageEntityByResultSet(ResultSet rs) throws SQLException {
    return StorageEntity.builder()
        .id(UUID.fromString(rs.getString("id")))
        .userId(UUID.fromString(rs.getString("user_id")))
        .mimeType(rs.getString("path"))
        .size(rs.getLong("file_size"))
        .path(rs.getString("path"))
        .visibility(rs.getString("visibility"))
        .isDeleted(rs.getBoolean("is_deleted"))
        .isDirectory(rs.getBoolean("is_directory"))
        .tags(FileTagsMapper.toList(rs.getString("tags")))
        .build();
  }
}
