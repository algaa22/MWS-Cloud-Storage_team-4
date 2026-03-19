package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
  private final FileErasureService erasureService;
  private final TariffService tariffService;

  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;

  private final NotificationClient notificationClient;

  public UUID simpleUpload(FileUploadRequest request) {
    UUID fileId = UUID.randomUUID();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    UUID parentId = request.parentId();
    String fileName = request.name();

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);

    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(parentId, fileName);
    }

    String mimeType = MimeTypeDetector.detect(fileName);
    byte[] data = request.data();

    StorageEntity entity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .parentId(parentId)
            .name(fileName)
            .isDirectory(false)
            .tags(request.tags())
            .status(FileStatus.READY)
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.increaseUsedStorage(userId, data.length);
    storageRepository.add(entity, data);

    checkStorageAndNotify(userId);
    return fileId;
  }

  public FileDownloadInfoDto download(ChunkedDownloadRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return new FileDownloadInfoDto(
        fileEntity.get().getMimeType(), storageRepository.download(entity), entity.getSize());
  }

  public void hardDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    UserEntity user =
        userRepository
            .getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    erasureService.hardDelete(entity);

    String fullPath = storageRepository.getFullFilePath(fileId);
    notificationClient.notifyFileDeleted(user.getEmail(), user.getUsername(), fullPath, userId);
  }

  @Transactional
  public void softDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .getIncludeDeleted(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    storageRepository.softDeleteEntity(fileEntity);
  }

  @Transactional
  public void restore(RestoreFileRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .getDeletedById(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    if (storageRepository.exists(userId, fileEntity.getParentId(), fileEntity.getName())) {
      throw new StorageFileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    storageRepository.restore(fileEntity);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getTrashFileList(GetFileListRequest request) {
    return storageRepository.getTrashFileList(request.userId(), request.parentId());
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId();
    UUID userId = request.userId();

    return storageRepository.getFileList(
        new FileListFilter(
            userId, parentId, request.includeDirectories(), request.recursive(), request.tags()));
  }

  @Transactional(readOnly = true)
  public StorageEntity getInfo(GetFileInfoRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    if (fileEntity.isEmpty()) {
      throw new StorageFileNotFoundException(fileId);
    }

    return fileEntity.get();
  }

  @Transactional
  public void changeMetadata(ChangeFileMetadataRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    if (storageRepository.exists(userId, request.newParentId(), request.newName())) {
      throw new StorageFileAlreadyExistsException(request.newParentId(), request.newName());
    }

    if (request.newName() != null) fileEntity.setName(request.newName());
    if (request.newParentId() != null) fileEntity.setParentId(request.newParentId());
    if (request.newTags() != null) fileEntity.setTags(request.newTags());
    if (request.newVisibility() != null) fileEntity.setVisibility(request.newVisibility().name());
  }
}
