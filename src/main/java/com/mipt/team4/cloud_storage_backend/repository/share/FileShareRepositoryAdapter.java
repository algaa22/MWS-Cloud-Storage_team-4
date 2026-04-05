package com.mipt.team4.cloud_storage_backend.repository.share;

import com.mipt.team4.cloud_storage_backend.exception.share.ShareNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class FileShareRepositoryAdapter {

  private final FileShareJpaRepository jpaRepository;

  @Transactional
  public FileShare save(FileShare share) {
    return jpaRepository.save(share);
  }

  @Transactional
  public void delete(FileShare share) {
    jpaRepository.delete(share);
  }

  @Transactional
  public void deleteById(UUID id) {
    jpaRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public Optional<FileShare> findById(UUID id) {
    return jpaRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public FileShare getById(UUID id) {
    return jpaRepository.findById(id).orElseThrow(() -> new ShareNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public Optional<FileShare> findByShareToken(String token) {
    return jpaRepository.findByShareToken(token);
  }

  @Transactional(readOnly = true)
  public FileShare getByShareToken(String token) {
    return jpaRepository
        .findByShareToken(token)
        .orElseThrow(() -> new ShareNotFoundException(token));
  }

  @Transactional(readOnly = true)
  public List<FileShare> findByFileId(UUID fileId) {
    return jpaRepository.findByFileId(fileId);
  }

  @Transactional(readOnly = true)
  public List<FileShare> findByCreatedById(UUID userId) {
    return jpaRepository.findByCreatedById(userId);
  }

  @Transactional(readOnly = true)
  public List<FileShare> findActiveSharesByFileId(UUID fileId) {
    return jpaRepository.findActiveSharesByFileId(fileId);
  }

  @Transactional(readOnly = true)
  public List<FileShare> findExpiredShares(LocalDateTime now) {
    return jpaRepository.findExpiredShares(now);
  }

  @Transactional
  public void deactivateShare(UUID shareId) {
    jpaRepository.deactivateShare(shareId);
  }

  @Transactional(readOnly = true)
  public boolean existsByShareToken(String token) {
    return jpaRepository.existsByShareToken(token);
  }

  @Transactional(readOnly = true)
  public long countActiveSharesByFileId(UUID fileId) {
    return jpaRepository.countActiveSharesByFileId(fileId);
  }

  @Transactional
  public Optional<FileShare> findExistingActiveShareByType(UUID fileId, UUID userId, FileShare.ShareType shareType) {
    if (shareType == FileShare.ShareType.PUBLIC) {
      return jpaRepository.findByFileIdAndCreatedByIdAndIsActiveTrueAndShareTypePublic(fileId, userId);
    } else {
      return jpaRepository.findByFileIdAndCreatedByIdAndIsActiveTrueAndShareTypeProtected(fileId, userId);
    }
  }
}
