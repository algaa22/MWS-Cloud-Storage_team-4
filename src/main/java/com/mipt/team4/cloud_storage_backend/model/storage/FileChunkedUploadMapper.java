package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileChunkedUploadEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class FileChunkedUploadMapper {
  public static FileChunkedUploadEntity toEntity(FileChunkedUploadDto dto) {
    return new FileChunkedUploadEntity(
        dto.sessionId(),
        dto.ownerId(),
        dto.totalFileSize(),
        dto.totalChunks(),
        null,
        dto.path(),
        dto.tags(),
        new ArrayList<>(),
        new ConcurrentHashMap<>());
  }
}
