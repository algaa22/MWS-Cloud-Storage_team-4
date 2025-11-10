package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.io.FileNotFoundException;
import java.util.*;

import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
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
  public void addFile(FileEntity fileEntity) {
    // TODO: написать проверку, есть ли файл с данным userId и path (не надеяться на сервис): если
    //       если уже есть файл, то бросать исключение FileAlreadyExistsException
    // TODO: может быть, стоит возвращать FileEntity?
    postgres.executeUpdate(
        "INSERT INTO files (id, owner_id, storage_path, file_size, mime_type, visibility, is_deleted, tags)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?);",
        List.of(
            fileEntity.getFileId(),
            fileEntity.getOwnerId(),
            fileEntity.getStoragePath(),
            fileEntity.getSize(),
            fileEntity.getMimeType(),
            fileEntity.getVisibility(),
            fileEntity.isDeleted(),
            FileTagsMapper.toString(fileEntity.getTags())));
  }

  @Override
  public Optional<FileEntity> getFile(UUID ownerId, String path) {
    List<FileEntity> result;

    result =
        postgres.executeQuery(
            "SELECT * FROM files WHERE owner_id = ? AND storage_path = ?;",
            List.of(ownerId, path),
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

  // TODO: в интерфейсе?
  public boolean fileExists(UUID ownerId, String storagePath) throws DbExecuteQueryException {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE owner_id = ? AND storage_path = ?);",
            List.of(ownerId, storagePath),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  // TODO: в интерфейсе?
  public void deleteFile(UUID ownerId, String storagePath)
      throws DbExecuteQueryException, FileNotFoundException, DbExecuteUpdateException {
    if (!fileExists(ownerId, storagePath)) {
        // TODO: обернуть в StorageFileNotFoundException
      throw new FileNotFoundException();
    }

    postgres.executeUpdate(
        "DELETE FROM files WHERE owner_id = ? AND storage_path = ?;",
        List.of(ownerId, storagePath));
  }
}
