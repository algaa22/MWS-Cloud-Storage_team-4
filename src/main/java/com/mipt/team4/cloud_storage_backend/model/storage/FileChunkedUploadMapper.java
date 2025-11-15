package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileChunkedUploadEntity;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileChunkedUploadMapper {
  public static FileChunkedUploadEntity toEntity(FileChunkedUploadDto dto) {
    return new FileChunkedUploadEntity(
        UUID.fromString(dto.sessionId()),
        UUID.fromString(dto.userToken()),
        dto.totalFileSize(),
        dto.totalChunks(),
        null,
        dto.path(),
        dto.tags(),
        new ArrayList<>(),
        new ConcurrentHashMap<>());
  }
}
