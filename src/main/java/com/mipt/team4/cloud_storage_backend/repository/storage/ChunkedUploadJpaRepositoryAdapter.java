package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPartEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ChunkedUploadJpaRepositoryAdapter {
  private final ChunkedUploadJpaRepository jpaRepository;

  @Transactional
  public void upsertPart(ChunkedUploadPartEntity part) {
    jpaRepository.upsertPart(part);
  }

  @Transactional
  public void addSession(ChunkedUploadSessionEntity session) {
    jpaRepository.saveAndFlush(session);
  }

  @Transactional
  public void deleteSession(UUID sessionId) {
    jpaRepository.deleteSessionById(sessionId);
  }

  @Transactional
  public void updateSessionStatus(
      UUID sessionId, ChunkedUploadStatus oldStatus, ChunkedUploadStatus newStatus) {
    jpaRepository.updateStatus(sessionId, oldStatus, newStatus);
  }

  @Transactional
  public void incrementCurrentSize(UUID sessionId, long delta) {
    jpaRepository.incrementCurrentSize(sessionId, delta);
  }

  @Transactional(readOnly = true)
  public Optional<ChunkedUploadSessionEntity> getSession(UUID sessionId) {
    return jpaRepository.findByIdWithParts(sessionId);
  }

  @Transactional(readOnly = true)
  public boolean isPartAlreadyUploaded(UUID sessionId, int partNumber) {
    return jpaRepository.existsPart(sessionId, partNumber);
  }
}
