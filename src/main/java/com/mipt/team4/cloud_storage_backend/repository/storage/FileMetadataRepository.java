package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {
  void addFile(FileEntity fileEntity) throws DbExecuteUpdateException, DbExecuteQueryException, FileAlreadyExistsException;

  Optional<FileEntity> getFile(UUID ownerID, String path) throws DbExecuteQueryException;
}
