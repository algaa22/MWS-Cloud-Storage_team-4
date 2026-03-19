package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.MoveDirectoryIntoItselfException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageDirectoryCycleException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UpdateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectoryService {
  private final StorageRepository storageRepository;
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final UserJpaRepositoryAdapter userRepository;

  @Transactional
  public UUID createDirectory(CreateDirectoryRequest request) {
    UUID userId = request.userId();
    UUID parentId = request.parentId();
    String name = request.name();

    if (storageRepository.exists(userId, parentId, name)) {
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

  @Transactional
  public void renameDirectory(UpdateDirectoryRequest request) {
    String newName = request.newName();
    UUID directoryId = request.id();
    UUID userId = request.userId();

    StorageEntity dirEntity = getDirectoryOrThrow(userId, directoryId);

    if (metadataRepository.exists(userId, dirEntity.getParentId(), newName)) {
      throw new StorageFileAlreadyExistsException(dirEntity.getParentId(), newName);
    }

    dirEntity.setName(newName);
    metadataRepository.saveFile(dirEntity);
  }

  @Transactional
  public void moveDirectory(UpdateDirectoryRequest request) {
    UUID newParentId = request.newParentId();
    UUID directoryId = request.id();
    UUID userId = request.userId();

    StorageEntity dir = getDirectoryOrThrow(userId, directoryId);

    if (directoryId.equals(newParentId)) {
      throw new MoveDirectoryIntoItselfException();
    }

    if (metadataRepository.isDescendant(directoryId, newParentId)) {
      throw new StorageDirectoryCycleException();
    }

    if (metadataRepository.exists(userId, newParentId, dir.getName())) {
      throw new StorageFileAlreadyExistsException(newParentId, dir.getName());
    }

    dir.setParentId(newParentId);
  }

  @Transactional
  public void deleteDirectory(DeleteDirectoryRequest request) {
    UUID directoryId = request.id();
    UUID userId = request.userId();

    StorageEntity directoryEntity = getDirectoryOrThrow(userId, directoryId);
    long freedSize = metadataRepository.calculateTotalSizeOfTree(directoryId);

    metadataRepository.hardDelete(directoryEntity.getUserId(), directoryEntity.getId());

    if (freedSize > 0) {
      userRepository.decreaseUsedStorage(userId, freedSize);
    }
  }

  private StorageEntity getDirectoryOrThrow(UUID userId, UUID directoryId) {
    return metadataRepository
        .get(userId, directoryId)
        .orElseThrow(() -> new StorageFileNotFoundException(directoryId));
  }
}
