package com.mipt.team4.cloudstorage.modules.storage.service;

import com.mipt.team4.cloudstorage.modules.storage.dto.FileDto;
import com.mipt.team4.cloudstorage.modules.storage.dto.FileUploadDto;
import java.util.List;

public interface FileService {
  // TODO: аналогично, как в UserService
  FileDto uploadFile(FileUploadDto uploadDto);

  byte[] downloadFile(String fileId);

  void deleteFile(String fileId);

  FileDto getFileInfo(String fileId);

  List<FileDto> listFilesByUser(String userId);
}
