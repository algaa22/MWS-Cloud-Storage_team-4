package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import java.util.*;
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
  public void addFile(FileEntity fileEntity) throws StorageFileAlreadyExistsException {
    if (fileExists(fileEntity.getOwnerId(), fileEntity.getS3Key()))
      throw new StorageFileAlreadyExistsException(
          fileEntity.getOwnerId(), fileEntity.getS3Key());

    postgres.executeUpdate(
        "INSERT INTO files (owner_id, storage_path, file_size, mime_type, visibility, is_deleted, tags)"
            + " values (?, ?, ?, ?, ?, ?, ?);",
        List.of(
            fileEntity.getOwnerId(),
            fileEntity.getS3Key(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            FileTagsMapper.toString(fileEntity.getTags())));
  }

  @Override
  public List<String> getFilesPathsList(UUID id) {
    return postgres.executeQuery(
        "SELECT storage_path FROM files WHERE owner_id = ? AND is_deleted = FALSE;",
        List.of(id),
        rs -> rs.getString("storage_path")
    );
  }

  @Override
  public Optional<FileEntity> getFile(UUID ownerId, String s3Key) {
    List<FileEntity> result;

    result =
        postgres.executeQuery(
            "SELECT * FROM files WHERE owner_id = ? AND storage_path = ?;",
            List.of(ownerId, s3Key),
            rs ->
                new FileEntity(
                    UUID.fromString(rs.getString("id")),
                    ownerId,
                    rs.getString("storage_path"),
                    rs.getString("mime_type"),
                    rs.getString("visibility"),
                    rs.getLong("file_size"),
                    rs.getBoolean("is_deleted"),
                    FileTagsMapper.toList(rs.getString("tags"))));

    if (result.isEmpty()) return Optional.empty();

    return Optional.ofNullable(result.getFirst());
  }

  @Override
  public boolean fileExists(UUID ownerId, String storagePath) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE owner_id = ? AND storage_path = ?);",
            List.of(ownerId, storagePath),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  @Override
  public void deleteFile(UUID ownerId, String path) throws StorageFileNotFoundException {
    if (!fileExists(ownerId, path)) {
      throw new StorageFileNotFoundException(path);
    }
    postgres.executeUpdate(
        "UPDATE files SET is_deleted = TRUE WHERE owner_id = ? AND path = ?;",
        List.of(ownerId, path)
    );
  }

}
