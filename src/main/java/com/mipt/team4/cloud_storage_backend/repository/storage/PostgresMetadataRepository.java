package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.util.*;

public class PostgresMetadataRepository implements FileMetadataRepository {
  PostgresConnection postgres;

  public PostgresMetadataRepository(PostgresConnection postgres) {
    this.postgres = postgres;
    postgres.connect(); // TODO: убрать в main class
  }

  @Override
  public void addFile(FileEntity fileEntity) {
    // TODO: написать проверку того, есть ли файл с данным ownerId и path
    postgres.executeUpdate(
        "INSERT INTO files (owner_id, storage_path, file_size, mime_type, visibility, is_deleted) values (?, ?, ?, ?, ?, ?);",
        List.of(fileEntity.getOwnerId(), fileEntity.getPath(), fileEntity.getSize(),
            fileEntity.getType(), fileEntity.getVisibility(), fileEntity.isDeleted()));
  }

  @Override
  public FileEntity getFile(UUID ownerId, String path) {
    String result = "";
    FileEntity fileEntity;
    fileEntity = postgres.executeQuery(
            "SELECT * FROM files WHERE owner_id = ? AND storage_path = ?;",
            List.of(ownerId, path),
            rs -> new FileEntity(UUID.fromString(rs.getString("id")), ownerId,
                rs.getString("storage_path"), rs.getString("mime_type"),
                rs.getString("visibility"), rs.getLong("file_size"),
                rs.getBoolean("is_deleted"),
                Arrays.asList(rs.getString("tags").split(", ")))).getFirst();

    return fileEntity;
  }
}
