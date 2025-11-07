package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileService {
  private final FileRepository repository;

  public FileService(FileRepository repository) {
    this.repository = repository;
  }

  public void startChunkedUploadSession(FileChunkedUploadSession chunkedUpload) {
    // TODO
  }

  public void processChunk(FileChunkDto fileChunkDto) {
    // TODO
  }

  public UUID finishChunkedUpload(Map<String, FileChunkedUploadSession> chunkedUploadSessions) {
    // TODO: возвращает fileId
    return null;
  }

  public FileDto uploadFile(String fileName, String contentType, InputStream fileStream) {
    return null;
  }

  public byte[] downloadFile(String fileId) {
    return null;
  }

  public void deleteFile(String fileId) {}

  public FileDto getFileInfo(String fileId) {
    return null;
  }

  public List<FileDto> listFilesByUser(String userId) {
    return null;
  }
}
