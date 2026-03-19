package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.S3Config;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.IncorrectPartNumberException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.IncorrectUploadStatusException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.MissingUploadPartsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooManyPartsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSizeMismatchException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CompleteChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.StartChunkedUploadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPartEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChunkedUploadService {
  private final TariffService tariffService;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationService notificationService;
  private final S3Config s3Config;

  @Transactional
  public StartChunkedUploadResponse startChunkedUpload(StartChunkedUploadRequest request) {
    UUID userId = request.userId();
    UUID parentId = request.parentId();
    String name = request.name();

    if (!tariffService.hasAccess(request.userId())) {
      throw new TariffAccessDeniedException();
    }

    if (request.totalParts() > s3Config.maxPartsNum()) {
      throw new TooManyPartsException(s3Config.maxPartsNum());
    }

    storageRepository
        .getIncludeDeleted(userId, parentId)
        .ifPresent(
            entity -> {
              throw new StorageFileAlreadyExistsException(parentId, name);
            });

    StorageEntity fileEntity =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mimeType(MimeTypeDetector.detect(name))
            .parentId(parentId)
            .name(name)
            .isDirectory(false)
            .tags(request.fileTags())
            .status(FileStatus.READY)
            .updatedAt(LocalDateTime.now())
            .build();

    UUID sessionId = UUID.randomUUID();

    ChunkedUploadSessionEntity session =
        ChunkedUploadSessionEntity.builder()
            .id(sessionId)
            .file(fileEntity)
            .status(ChunkedUploadStatus.UPLOADING)
            .totalParts(request.totalParts())
            .build();

    userRepository.increaseUsedStorage(userId, request.fileSize());
    storageRepository.startMultipartUpload(fileEntity, session);

    return new StartChunkedUploadResponse(sessionId);
  }

  @Transactional
  public void uploadPart(ChunkedUploadPartDto partDto) {
    ChunkedUploadSessionEntity session = getSession(partDto.sessionId());

    validatePartNumber(partDto.partNumber(), session.getTotalParts());
    validatePartSize(partDto.size(), partDto.partNumber(), session.getTotalParts());
    validateCurrentFileSize(session.getCurrentSize(), partDto.size(), session.getFile().getSize());

    ChunkedUploadPartEntity partEntity =
        ChunkedUploadPartEntity.builder()
            .id(UUID.randomUUID())
            .session(session)
            .partNumber(partDto.partNumber())
            .size(partDto.size())
            .build();

    storageRepository.uploadPart(
        session.getFile(),
        session.getId(),
        session.getUploadId(),
        partDto.inputStream(),
        partEntity);
  }

  @Transactional
  public UUID completeChunkedUpload(CompleteChunkedUploadRequest request) {
    ChunkedUploadSessionEntity session = getSession(request.sessionId());

    storageRepository.updateUploadSessionStatus(
        session.getId(), session.getStatus(), ChunkedUploadStatus.COMPLETING);

    try {
      StorageEntity fileEntity = session.getFile();
      Map<Integer, String> partETags = collectETagsIntoMap(session.getParts());

      validateFinalFileSize(session);
      checkMissingParts(session, partETags);

      storageRepository.completeMultipartUpload(
          fileEntity, session.getId(), session.getUploadId(), partETags);
      notificationService.checkStorageUsageAndNotify(fileEntity.getUserId());

      return fileEntity.getId();
    } catch (Exception e) {
      storageRepository.updateUploadSessionStatus(
          session.getId(), ChunkedUploadStatus.COMPLETING, ChunkedUploadStatus.UPLOADING);
      throw e;
    }
  }

  private ChunkedUploadSessionEntity getSession(UUID sessionId) {
    ChunkedUploadSessionEntity session =
        storageRepository
            .getUploadSession(sessionId)
            .orElseThrow(UploadSessionNotFoundException::new);

    if (session.getStatus() != ChunkedUploadStatus.UPLOADING) {
      throw new IncorrectUploadStatusException(ChunkedUploadStatus.UPLOADING, session.getStatus());
    }

    return session;
  }

  private Map<Integer, String> collectETagsIntoMap(List<ChunkedUploadPartEntity> parts) {
    return parts.stream()
        .collect(
            Collectors.toMap(
                ChunkedUploadPartEntity::getPartNumber,
                ChunkedUploadPartEntity::getETag,
                (oldValue, newValue) -> oldValue,
                TreeMap::new));
  }

  private void validatePartNumber(int partNumber, int totalParts) {
    if (partNumber < 1 || partNumber > totalParts) {
      throw new IncorrectPartNumberException(partNumber, totalParts);
    }
  }

  private void validateCurrentFileSize(long currentSize, long partSize, long expectedSize) {
    long newSize = currentSize + partSize;

    if (newSize > expectedSize) {
      throw new UploadSizeMismatchException(expectedSize, newSize);
    }
  }

  private void validatePartSize(long partSize, int partNumber, int totalParts) {
    if (partNumber < totalParts && partSize < s3Config.minFilePartSize()) {
      throw new TooSmallFilePartException(s3Config.minFilePartSize());
    }
  }

  private void validateFinalFileSize(ChunkedUploadSessionEntity session) {
    StorageEntity fileEntity = session.getFile();

    if (fileEntity.getSize() != session.getCurrentSize()) {
      throw new UploadSizeMismatchException(fileEntity.getSize(), session.getCurrentSize());
    }
  }

  private void checkMissingParts(
      ChunkedUploadSessionEntity session, Map<Integer, String> partETags) {
    if (session.getTotalParts() != partETags.size()) {
      BitSet partsBitSet = new BitSet(session.getTotalParts());

      for (int partNumber : partETags.keySet()) {
        partsBitSet.set(partNumber - 1);
      }

      throw new MissingUploadPartsException(partsBitSet);
    }
  }

  public boolean isPartAlreadyUploaded(ChunkedUploadPartRequest request) {
    return storageRepository.isPartAlreadyUploaded(request.sessionId(), request.part());
  }
}
