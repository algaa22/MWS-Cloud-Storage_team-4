package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageDirectoryCycleException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.MoveDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DirectoryService {

  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final FileMetadataRepository metadataRepository;
  private final UserRepository userRepository;

  public UUID createDirectory(CreateDirectoryRequest request) {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
    String name = request.name();

    if (storageRepository.fileExists(userId, parentId, name)) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    UUID directoryId = UUID.randomUUID();
    StorageEntity directoryEntity =
        StorageEntity.builder()
            .id(directoryId)
            .userId(userId)
            .mimeType("application/x-directory")
            .size(0)
            .parentId(parentId)
            .name(name)
            .isDirectory(true)
            .updatedAt(LocalDateTime.now())
            .tags(List.of())
            .build();

    storageRepository.addDirectory(directoryEntity);

    return directoryId;
  }

  public void renameDirectory(RenameDirectoryRequest request) {
    String newName = request.newName();
    UUID directoryId = UUID.fromString(request.directoryId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity dirEntity = getDirectoryOrThrow(userId, directoryId);

    if (metadataRepository.fileExists(userId, dirEntity.getParentId(), newName)) {
      throw new StorageFileAlreadyExistsException(dirEntity.getParentId(), newName);
    }

    dirEntity.setName(newName);
    metadataRepository.updateEntity(dirEntity);
  }

  public void moveDirectory(MoveDirectoryRequest request) {
    UUID newParentId = UUID.fromString(request.newParentId());
    UUID directoryId = UUID.fromString(request.directoryId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity dir = getDirectoryOrThrow(userId, directoryId);

    if (directoryId.equals(newParentId)) {
      throw new IllegalArgumentException("Cannot move directory into itself");
    }

    if (metadataRepository.isDescendant(directoryId, newParentId)) {
      throw new StorageDirectoryCycleException("Cannot move directory into its own sub-directory");
    }

    if (metadataRepository.fileExists(userId, newParentId, dir.getName())) {
      throw new StorageFileAlreadyExistsException(newParentId, dir.getName());
    }

    dir.setParentId(newParentId);
    metadataRepository.updateEntity(dir);
  }

  public void deleteDirectory(DeleteDirectoryRequest request) {
    UUID directoryId = UUID.fromString(request.directoryId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity directoryEntity = getDirectoryOrThrow(userId, directoryId);
    long freedSize = metadataRepository.calculateTotalSizeOfTree(directoryId);

    metadataRepository.deleteFile(directoryEntity);

    if (freedSize > 0) {
      userRepository.decreaseUsedStorage(userId, freedSize);
    }
  }

  private StorageEntity getDirectoryOrThrow(UUID userId, UUID directoryId) {
    return metadataRepository
        .getFile(userId, directoryId)
        .orElseThrow(() -> new StorageFileNotFoundException(directoryId));
  }
}
