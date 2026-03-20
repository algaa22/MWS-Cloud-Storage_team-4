package com.mipt.team4.cloud_storage_backend.repository.share;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileShareJpaRepository extends JpaRepository<FileShare, UUID> {

  Optional<FileShare> findByShareToken(String shareToken);

  List<FileShare> findByFileId(UUID fileId);

  List<FileShare> findByCreatedById(UUID userId);

  @Query("SELECT fs FROM FileShare fs WHERE fs.file.id = :fileId AND fs.isActive = true")
  List<FileShare> findActiveSharesByFileId(@Param("fileId") UUID fileId);

  @Query("SELECT fs FROM FileShare fs WHERE fs.expiresAt < :now AND fs.isActive = true")
  List<FileShare> findExpiredShares(@Param("now") LocalDateTime now);

  @Modifying
  @Query("UPDATE FileShare fs SET fs.isActive = false WHERE fs.id = :shareId")
  void deactivateShare(@Param("shareId") UUID shareId);

  boolean existsByShareToken(String shareToken);

  @Query("SELECT COUNT(fs) FROM FileShare fs WHERE fs.file.id = :fileId AND fs.isActive = true")
  long countActiveSharesByFileId(@Param("fileId") UUID fileId);
}
