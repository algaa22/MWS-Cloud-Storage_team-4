package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageDirectoryCycleException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectoryService {

  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final FileMetadataRepository metadataRepository;
  private final UserRepository userRepository;

  public void createDirectory(CreateDirectoryRequest createDirectory)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(createDirectory.userToken());
    UUID parentId = createDirectory.parentId();
    String name = createDirectory.name();

    if (storageRepository.fileExists(userId, parentId, name)) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    StorageEntity directoryEntity =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mimeType("application/x-directory")
            .size(0)
            .parentId(parentId)
            .name(name)
            .isDirectory(true)
            .tags(List.of())
            .build();

    storageRepository.addDirectory(directoryEntity);
  }

  public void renameDirectory(String userToken, UUID directoryId, String newName)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(userToken);
    StorageEntity dir = getDirectoryOrThrow(userId, directoryId);

    if (metadataRepository.fileExists(userId, dir.getParentId(), newName)) {
      throw new StorageFileAlreadyExistsException(dir.getParentId(), newName);
    }

    dir.setName(newName);
    metadataRepository.updateEntity(dir);
  }

  public void moveDirectory(String userToken, UUID directoryId, UUID newParentId)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(userToken);
    StorageEntity dir = getDirectoryOrThrow(userId, directoryId);

    if (directoryId.equals(newParentId)) {
      throw new IllegalArgumentException("Cannot move directory into itself");
    }

    if (newParentId != null) {
      if (metadataRepository.isDescendant(directoryId, newParentId)) {
        throw new StorageDirectoryCycleException(
            "Cannot move directory into its own sub-directory");
      }
    }

    if (metadataRepository.fileExists(userId, newParentId, dir.getName())) {
      throw new StorageFileAlreadyExistsException(newParentId, dir.getName());
    }

    dir.setParentId(newParentId);
    metadataRepository.updateEntity(dir);
  }

  @Transactional
  public void deleteDirectory(String userToken, UUID directoryId)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(userToken);

    StorageEntity directoryEntity = getDirectoryOrThrow(userId, directoryId);

    long freedSize = metadataRepository.calculateTotalSizeOfTree(directoryId);

    metadataRepository.deleteFile(userId, directoryEntity.getParentId(), directoryEntity.getName());

    if (freedSize > 0) {
      userRepository.decreaseUsedStorage(userId, freedSize);
    }
  }

  private StorageEntity getDirectoryOrThrow(UUID userId, UUID directoryId) {
    return metadataRepository
        .getFile(directoryId)
        .filter(f -> f.getUserId().equals(userId))
        .filter(StorageEntity::isDirectory)
        .orElseThrow(() -> new StorageFileNotFoundException(directoryId));
  }
}
