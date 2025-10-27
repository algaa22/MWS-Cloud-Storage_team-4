package service;

import dto.FileGetDto;
import dto.FileUploadDto;
import java.util.List;

public interface FileService {
  // Загрузить файл
  FileGetDto uploadFile(FileUploadDto uploadDto);

  // Скачать файл
  byte[] downloadFile(String fileId);

  // Удалить файл
  void deleteFile(String fileId);

  // Получить информацию о файле
  FileGetDto getFileInfo(String fileId);

  // Получить список файлов пользователя по его id
  List<FileGetDto> listFilesByUser(String userId);
}
