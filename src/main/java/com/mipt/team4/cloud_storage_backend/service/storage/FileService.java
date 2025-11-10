package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadInfo;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileInfo;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import java.io.InputStream;
import java.util.List;

public class FileService {
  private final FileRepository repository;

  public FileService(FileRepository repository) {
    this.repository = repository;
  }

  public void startChunkedUploadSession(FileChunkedUploadSession chunkedUpload) {
    // TODO: можно ли юзеру загрузить (квоты)
  }

  public void processChunk(FileChunk fileChunk) {
    // TODO: поручить FileRepository сохранить чанк, проверять место вообще есть
  }

  public String finishChunkedUpload(FileChunkedUploadSession chunkedUploadSession) {
    // TODO: возвращает fileId, поручает FileRepository сохранить последний чанк
    return null;
  }

  public FileChunkedDownloadInfo getFileDownloadInfo(String fileId, String userId) {
    // TODO
    return null;
  }

  public FileInfo uploadFile(String fileName, String contentType, InputStream fileStream) {
    return null;
  }

  public FileChunk getFileChunk(String currentFileId, int chunkIndex, int chunkSize) {
    return null;
  }

  public byte[] downloadFile(String fileId) {
    return null;
  }

  public void deleteFile(String fileId) {}

  public FileInfo getFileInfo(String fileId, String userId) {
    return null;
  }

  public List<FileInfo> listFilesByUser(String userId) {
    return null;
  }

  public List<String> getFilePathsList(String userId) {
    return null;
  }
}
