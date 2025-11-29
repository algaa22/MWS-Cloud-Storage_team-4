package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeDirectoryPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleDirectoryOperationDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DirectoryService {
  private final StorageRepository storageRepository;
  private final UserSessionService userSessionService;

  public DirectoryService(
      StorageRepository storageRepository, UserSessionService userSessionService) {
    this.storageRepository = storageRepository;
    this.userSessionService = userSessionService;
  }

  public void createDirectory(SimpleDirectoryOperationDto createDirectory)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(createDirectory.userToken());
    String directoryPath = createDirectory.directoryPath();

    if (storageRepository.fileExists(userId, directoryPath))
      throw new StorageFileAlreadyExistsException(directoryPath);

    StorageEntity directoryEntity =
        new StorageEntity(
            UUID.randomUUID(),
            userId,
            directoryPath,
            "application/x-directory", // TODO: hardcoding
            "private", // TODO: hardcoding, нужно создать enum
            0,
            false,
            List.of(),
            true);

    storageRepository.addDirectory(directoryEntity);
  }

  public void changeDirectoryPath(ChangeDirectoryPathDto changeDirectory)
      throws UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(changeDirectory.userToken());
    String oldPath = changeDirectory.oldDirectoryPath();
    String newPath = changeDirectory.newDirectoryPath();

    Map<StorageEntity, String> changedDirectoryFilePaths = new HashMap<>();

    for (String oldFilePath : storageRepository.getFilePathsList(userId, false, oldPath)) {
      Optional<StorageEntity> fileOpt = storageRepository.getFile(userId, oldFilePath);
      if (fileOpt.isEmpty()) throw new StorageFileNotFoundException(oldFilePath);

      String newFilePath = oldFilePath.replaceFirst(oldPath, newPath);
      if (storageRepository.fileExists(userId, newFilePath))
        throw new StorageFileAlreadyExistsException(newFilePath);

      changedDirectoryFilePaths.put(fileOpt.get(), newFilePath);
    }

    for (Map.Entry<StorageEntity, String> entry : changedDirectoryFilePaths.entrySet()) {
      StorageEntity fileEntity = entry.getKey();
      String newFilePath = entry.getValue();

      fileEntity.setPath(newFilePath);
      storageRepository.updateFile(fileEntity);
    }
  }

  public void deleteDirectory(SimpleDirectoryOperationDto request)
      throws UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    String directoryPath = request.directoryPath();

    List<String> directoryFiles = storageRepository.getFilePathsList(userId, false, directoryPath);

    for (String filePath : directoryFiles) {
      storageRepository.deleteFile(userId, filePath);
    }
  }
}
