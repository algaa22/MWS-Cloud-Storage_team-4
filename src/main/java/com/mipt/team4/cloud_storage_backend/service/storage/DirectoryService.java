package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeDirectoryPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleDirectoryOperationDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DirectoryService {

  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final UserRepository userRepository;

  public DirectoryService(
      StorageRepository storageRepository, UserRepository userRepository,
      UserSessionService userSessionService) {
    this.storageRepository = storageRepository;
    this.userRepository = userRepository;
    this.userSessionService = userSessionService;
  }

  public void createDirectory(SimpleDirectoryOperationDto createDirectory)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(createDirectory.userToken());
    String directoryPath = createDirectory.directoryPath();

    if (storageRepository.fileExists(userId, directoryPath)) {
      throw new StorageFileAlreadyExistsException(directoryPath);
    }

    StorageEntity directoryEntity =
        new StorageEntity(
            UUID.randomUUID(),
            userId,
            directoryPath,
            "application/x-directory",
            0,
            List.of(),
            true);

    storageRepository.addDirectory(directoryEntity);
  }

  public void changeDirectoryPath(ChangeDirectoryPathDto changeDirectory)
      throws UserNotFoundException,
      StorageFileAlreadyExistsException,
      StorageEntityNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(changeDirectory.userToken());
    String oldDirectoryPath = changeDirectory.oldDirectoryPath();
    String newDirectoryPath = changeDirectory.newDirectoryPath();

    List<StorageEntity> directoryFiles =
        storageRepository.getFileList(new FileListFilter(userId, true, true, oldDirectoryPath));

    for (StorageEntity oldFile : directoryFiles) {
      String oldFilePath = oldFile.getPath();
      Optional<StorageEntity> fileOpt = storageRepository.getFile(userId, oldFilePath);
      if (fileOpt.isEmpty()) {
        throw new StorageEntityNotFoundException(oldFilePath);
      }

      String newFilePath = oldFilePath.replaceFirst(oldDirectoryPath, newDirectoryPath);
      if (storageRepository.fileExists(userId, newFilePath)) {
        throw new StorageFileAlreadyExistsException(newFilePath);
      }

      StorageEntity fileEntity = fileOpt.get();
      fileEntity.setPath(newFilePath);
      storageRepository.updateFile(fileEntity);
    }
  }

  public void deleteDirectory(SimpleDirectoryOperationDto request)
      throws UserNotFoundException, StorageEntityNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    String directoryPath = request.directoryPath();

    Optional<StorageEntity> directoryEntity = storageRepository.getFile(userId, directoryPath);
    if (directoryEntity.isEmpty()) {
      throw new StorageEntityNotFoundException(directoryPath);
    }

    storageRepository.deleteFile(directoryEntity.orElse(null));

    List<StorageEntity> directoryFiles = storageRepository.getFileList(
        new FileListFilter(userId, true, true, directoryPath));

    for (StorageEntity file : directoryFiles) {
      userRepository.decreaseUsedStorage(userId, file.getSize());
      storageRepository.deleteFile(userId, file.getPath());
    }
  }
}
