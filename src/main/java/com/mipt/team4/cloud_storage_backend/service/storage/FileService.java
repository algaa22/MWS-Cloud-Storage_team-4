package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import java.io.InputStream;
import java.util.List;

public interface FileService {

  FileDto uploadFile(String fileName, String contentType, InputStream fileStream);

  byte[] downloadFile(String fileId);

  void deleteFile(String fileId);

  FileDto getFileInfo(String fileId);

  List<FileDto> listFilesByUser(String userId);
}
