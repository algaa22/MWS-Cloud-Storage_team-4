package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresFileMetadataRepository implements FileMetadataRepository {

  private static final Logger logger =
      LoggerFactory.getLogger(PostgresFileMetadataRepository.class);

  PostgresConnection postgres;

  public PostgresFileMetadataRepository(PostgresConnection postgres) {
    this.postgres = postgres;
  }

  @Override
  public void addFile(StorageEntity fileEntity) throws StorageFileAlreadyExistsException {
    if (fileExists(fileEntity.getUserId(), fileEntity.getPath())) {
      throw new StorageFileAlreadyExistsException(fileEntity.getPath());
    }

    postgres.executeUpdate(
        "INSERT INTO files (id, owner_id, path, file_size, mime_type, visibility, is_deleted, tags, is_directory)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?);",
        List.of(
            fileEntity.getEntityId(),
            fileEntity.getUserId(),
            fileEntity.getPath(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.isDirectory()));
  }

  @Override
  public List<StorageEntity> getFilesList(FileListFilter filter) {
    String query =
        "SELECT * FROM files WHERE owner_id = ? AND path LIKE ? AND path != ? AND is_deleted = FALSE";
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

    return postgres.executeQuery(
        query,
        params,
        rs -> resultToStorageEntity(filter.userId(), rs));
  }

  @Override
  public Optional<StorageEntity> getFile(UUID userId, String path) {
    List<StorageEntity> result;

    result =
        postgres.executeQuery(
            "SELECT * FROM files WHERE owner_id = ? AND path = ?;",
            List.of(userId, path),
            rs -> resultToStorageEntity(userId, rs));

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(result.getFirst());
  }

  @Override
  public void deleteFile(UUID userId, String path) throws StorageEntityNotFoundException {
    if (!fileExists(userId, path)) {
      throw new StorageEntityNotFoundException(path);
    }

    postgres.executeUpdate(
        "DELETE FROM files WHERE owner_id = ? AND path = ?;", List.of(userId, path));
  }

  @Override
  public void updateFile(StorageEntity fileEntity) {
    postgres.executeUpdate(
        "UPDATE files SET path = ?, visibility = ?, tags = ? WHERE owner_id = ? AND id = ?",
        List.of(
            fileEntity.getPath(),
            fileEntity.getVisibility(),
            FileTagsMapper.toString(fileEntity.getTags()),
            fileEntity.getUserId(),
            fileEntity.getEntityId()));
  }

  @Override
  public boolean fileExists(UUID userId, String path) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE owner_id = ? AND path = ?);",
            List.of(userId, path),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  private StorageEntity resultToStorageEntity(UUID userId, ResultSet rs) throws SQLException {
    return new StorageEntity(
        UUID.fromString(rs.getString("id")),
        userId,
        rs.getString("path"),
        rs.getString("mime_type"),
        rs.getString("visibility"),
        rs.getLong("file_size"),
        rs.getBoolean("is_deleted"),
        FileTagsMapper.toList(rs.getString("tags")),
        rs.getBoolean("is_directory"));
  }
}
