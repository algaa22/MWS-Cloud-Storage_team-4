package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import java.io.InputStream;
import java.util.List;

public class FileService {

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
