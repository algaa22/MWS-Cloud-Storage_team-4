package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import java.util.List;

public interface FileService {
  // TODO: аналогично, как в UserService
  FileDto uploadFile(FileUploadDto uploadDto);

  byte[] downloadFile(String fileId);

  void deleteFile(String fileId);

  FileDto getFileInfo(String fileId);

  List<FileDto> listFilesByUser(String userId);
}
