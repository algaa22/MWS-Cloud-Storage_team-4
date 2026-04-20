package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import com.mipt.team4.cloud_storage_backend.antivirus.messaging.AntivirusTaskProducer;
import com.mipt.team4.cloud_storage_backend.antivirus.model.mapper.ScanTaskMapper;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.upload.*;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadStatusDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.AbortUploadSessionRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CompleteChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetUploadStatusRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.StartChunkedUploadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPartEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.file.HashUtils;
import com.mipt.team4.cloud_storage_backend.utils.string.MimeTypeDetector;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ChunkedUploadService {
  private final TariffService tariffService;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationService notificationService;
  private final AntivirusTaskProducer antivirusTaskProducer;
  private final StorageProps storageProps;
  private final AntivirusProps antivirusProps;

  @Transactional
  public StartChunkedUploadResponse startUpload(StartChunkedUploadRequest request) {
    UUID userId = request.userId();
    UUID parentId = request.parentId();
    String name = request.name();

    if (!tariffService.hasAccess(request.userId())) {
      throw new TariffAccessDeniedException();
    }

    if (request.totalParts() > storageProps.s3().limits().maxPartsNum()) {
      throw new TooManyPartsException(storageProps.s3().limits().maxPartsNum());
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
            .updatedAt(LocalDateTime.now())
            .size(request.fileSize())
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
    storageRepository.startChunkedUpload(fileEntity, session);

    return new StartChunkedUploadResponse(sessionId);
  }

  @Transactional
  public void abortUpload(AbortUploadSessionRequest request) {
    ChunkedUploadSessionEntity session = getUploadingSession(request.sessionId());

    storageRepository.tryUpdateUploadSessionStatus(
        session, ChunkedUploadStatus.UPLOADING, ChunkedUploadStatus.ABORTING);

    try {
      forceAbortUpload(session);
    } catch (Exception e) {
      storageRepository.tryUpdateUploadSessionStatus(
          session, ChunkedUploadStatus.ABORTING, ChunkedUploadStatus.UPLOADING);
      throw e;
    }
  }

  @Transactional
  public void forceAbortUpload(ChunkedUploadSessionEntity session) {
    StorageEntity fileEntity = session.getFile();

    storageRepository.abortMultipartUpload(fileEntity, session.getId(), session.getUploadId());
    userRepository.decreaseUsedStorage(fileEntity.getUserId(), fileEntity.getSize());
  }

  @Transactional
  public void uploadPart(ChunkedUploadPartDto partDto) {
    ChunkedUploadSessionEntity session = getUploadingSession(partDto.sessionId());

    validateIfChecksumSpecified(partDto.checksum());
    validatePartNumber(partDto.partNumber(), session.getTotalParts());
    validatePartSize(partDto.size(), partDto.partNumber(), session.getTotalParts());
    validateCurrentFileSize(session.getCurrentSize(), partDto.size(), session.getFile().getSize());

    ChunkedUploadPartEntity partEntity =
        ChunkedUploadPartEntity.builder()
            .id(UUID.randomUUID())
            .session(session)
            .number(partDto.partNumber())
            .size(partDto.size())
            .hash(partDto.checksum())
            .build();

    MessageDigest messageDigest = HashUtils.createSha256();

    try (InputStream inputStream =
        partDto.checksum() == null
            ? partDto.inputStream()
            : new DigestInputStream(partDto.inputStream(), messageDigest)) {
      storageRepository.uploadPart(
          session.getFile(), session.getId(), session.getUploadId(), inputStream, partEntity);

      if (partDto.checksum() != null) {
        HashUtils.compareChecksums(partDto.checksum(), messageDigest.digest());
      }
    } catch (IOException e) {
      throw new UploadPartIOException(e);
    }
  }

  @Transactional
  public UUID completeUpload(CompleteChunkedUploadRequest request) {
    ChunkedUploadSessionEntity session = getUploadingSession(request.sessionId());

    storageRepository.tryUpdateUploadSessionStatus(
        session, ChunkedUploadStatus.UPLOADING, ChunkedUploadStatus.COMPLETING);

    try {
      StorageEntity fileEntity = session.getFile();
      Map<Integer, String> partETags = collectETagsIntoMap(session.getParts());

      checkMissingParts(session, partETags);
      validateFinalFileSize(session);

      fileEntity.setHash(computeFinalFileHash(session.getParts()));

      storageRepository.completeMultipartUpload(
          fileEntity, session.getId(), session.getUploadId(), partETags);
      notificationService.checkStorageUsageAndNotify(fileEntity.getUserId());

      sendAntivirusScanTask(fileEntity);

      return fileEntity.getId();
    } catch (Exception e) {
      storageRepository.tryUpdateUploadSessionStatus(
          session, ChunkedUploadStatus.COMPLETING, ChunkedUploadStatus.UPLOADING);
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public UploadStatusDto getUploadStatus(GetUploadStatusRequest request) {
    UUID sessionId = request.sessionId();
    ChunkedUploadSessionEntity session = getSession(sessionId);

    Map<Integer, String> partETags = collectETagsIntoMap(session.getParts());
    BitSet missingPartsBitSet = getMissingPartsBitSet(session, partETags);

    return new UploadStatusDto(
        sessionId,
        session.getStatus(),
        session.getCurrentSize(),
        partETags.size(),
        missingPartsBitSet);
  }

  public boolean isPartAlreadyUploaded(ChunkedUploadPartRequest request) {
    return storageRepository.isPartAlreadyUploaded(request.sessionId(), request.part());
  }

  private String computeFinalFileHash(List<ChunkedUploadPartEntity> parts) {
    for (ChunkedUploadPartEntity part : parts) {
      if (part.getHash() == null) {
        return null;
      }
    }

    List<ChunkedUploadPartEntity> sortedParts =
        parts.stream().sorted(Comparator.comparingInt(ChunkedUploadPartEntity::getNumber)).toList();
    MessageDigest digest = HashUtils.createSha256();

    for (ChunkedUploadPartEntity part : sortedParts) {
      HashUtils.update(digest, part.getHash());
    }

    return HashUtils.encodeDigest(digest.digest());
  }

  private ChunkedUploadSessionEntity getUploadingSession(UUID sessionId) {
    ChunkedUploadSessionEntity session = getSession(sessionId);

    if (session.getStatus() != ChunkedUploadStatus.UPLOADING) {
      throw new IncorrectUploadStatusException(ChunkedUploadStatus.UPLOADING, session.getStatus());
    }

    return session;
  }

  private ChunkedUploadSessionEntity getSession(UUID sessionId) {
    return storageRepository
        .getUploadSession(sessionId)
        .orElseThrow(UploadSessionNotFoundException::new);
  }

  private void sendAntivirusScanTask(StorageEntity fileEntity) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            antivirusTaskProducer.sendTask(ScanTaskMapper.toTask(fileEntity));
          }
        });
  }

  private Map<Integer, String> collectETagsIntoMap(List<ChunkedUploadPartEntity> parts) {
    return parts.stream()
        .collect(
            Collectors.toMap(
                ChunkedUploadPartEntity::getNumber,
                ChunkedUploadPartEntity::getETag,
                (oldValue, newValue) -> oldValue,
                TreeMap::new));
  }

  private void validateIfChecksumSpecified(String checksum) {
    if (antivirusProps.enabled() && checksum == null) {
      throw new MissingChecksumException();
    }
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
    if (partNumber < totalParts && partSize < storageProps.s3().limits().minFilePartSize()) {
      throw new TooSmallFilePartException(storageProps.s3().limits().minFilePartSize());
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
    BitSet partsBitSet = getMissingPartsBitSet(session, partETags);

    if (partsBitSet != null) {
      throw new MissingUploadPartsException(partsBitSet);
    }
  }

  private BitSet getMissingPartsBitSet(
      ChunkedUploadSessionEntity session, Map<Integer, String> partETags) {
    if (session.getTotalParts() == partETags.size()) {
      return null;
    }

    BitSet partsBitSet = new BitSet(session.getTotalParts());

    for (int partNumber : partETags.keySet()) {
      partsBitSet.set(partNumber - 1);
    }

    return partsBitSet;
  }
}
