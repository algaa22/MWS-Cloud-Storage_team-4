package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.common.mappers.PaginationMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ContentRangeDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.TrashFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.converter.ContentRangeConverter;
import com.mipt.team4.cloud_storage_backend.utils.file.HashUtils;
import com.mipt.team4.cloud_storage_backend.utils.string.MimeTypeDetector;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
  private final NotificationService notificationService;

  @Transactional
  public UUID simpleUpload(SimpleUploadRequest request) {
    UUID userId = request.userId();
    UUID parentId = request.parentId();
    String name = request.name();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException();
    }

    storageRepository
        .getIncludeDeleted(userId, parentId, name)
        .ifPresent(
            entity -> {
              throw new StorageFileAlreadyExistsException(parentId, name);
            });

    UUID fileId = UUID.randomUUID();
    String mimeType = MimeTypeDetector.detect(name);
    byte[] data = request.data();

    if (request.checksum() != null) {
      MessageDigest messageDigest = HashUtils.createSha256();
      HashUtils.compareChecksums(request.checksum(), messageDigest.digest(data));
    }

    StorageEntity fileEntity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .parentId(parentId)
            .name(name)
            .isDirectory(false)
            .tags(request.tags())
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.increaseUsedStorage(userId, data.length);
    storageRepository.add(fileEntity, data);
    notificationService.checkStorageUsageAndNotify(userId);

    return fileId;
  }

  public FileDownloadInfoDto download(ChunkedDownloadRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException();
    }

    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    String rangeStr = request.range();
    ContentRangeDto rangeDto =
        rangeStr != null
            ? ContentRangeConverter.fromClientRange(rangeStr, fileEntity.getSize())
            : null;

    InputStream inputStream = storageRepository.download(fileEntity, rangeStr);

    return new FileDownloadInfoDto(
        fileEntity.getMimeType(), inputStream, rangeDto, fileEntity.getSize());
  }

  @Transactional
  public void hardDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .getIncludeDeleted(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    UserEntity userEntity =
        userRepository
            .getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    erasureService.hardDelete(fileEntity);
    notificationService.notifyFileDeleted(fileId, userEntity);
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
            .getDeleted(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    if (storageRepository.exists(userId, fileEntity.getParentId(), fileEntity.getName())) {
      throw new StorageFileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    storageRepository.restore(fileEntity);
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getTrashFileList(TrashFileListRequest request) {
    return storageRepository.getTrashFileList(
        request.userId(), request.parentId(), PaginationMapper.toPageQuery(request.pagination()));
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId();
    UUID userId = request.userId();

    return storageRepository.getFileList(
        new FileListFilter(
            userId, parentId, request.includeDirectories(), request.recursive(), request.tags()),
        PaginationMapper.toPageQuery(request.pagination()));
  }

  @Transactional(readOnly = true)
  public StorageEntity getInfo(GetFileInfoRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    return storageRepository
        .get(userId, fileId)
        .orElseThrow(() -> new StorageFileNotFoundException(fileId));
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
