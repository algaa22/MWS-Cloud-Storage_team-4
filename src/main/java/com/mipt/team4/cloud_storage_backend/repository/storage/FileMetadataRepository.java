package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class FileMetadataRepository {
  private final StorageJpaRepository jpaRepository;
  private final EntityManager entityManager;

  @Transactional
  public void addFile(StorageEntity fileEntity) {
    if (fileEntity.getId() == null) fileEntity.setId(UUID.randomUUID());

    String tagsStr = FileTagsMapper.toString(fileEntity.getTags());
    jpaRepository.upsertFile(fileEntity, tagsStr);
  }

  public List<StorageEntity> getStaleFiles(LocalDateTime threshold) {
    return jpaRepository.findByStatusInAndUpdatedAtBefore(
        List.of(FileStatus.PENDING, FileStatus.ERROR), threshold);
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<StorageEntity> getFileList(FileListFilter filter) {
    StringBuilder sql = new StringBuilder();

    if (filter.recursive()) {
      sql.append(
          """
            WITH RECURSIVE folder_tree AS (
                SELECT * FROM files
                WHERE user_id = :userId
                  AND parent_id IS NOT DISTINCT FROM CAST(:parentId AS UUID)
                  AND is_deleted = FALSE
                UNION ALL
                SELECT f.* FROM files f
                INNER JOIN folder_tree ft ON f.parent_id = ft.id
                WHERE f.is_deleted = FALSE
            )
            SELECT * FROM folder_tree WHERE 1=1
        """);
    } else {
      sql.append(
          """
            SELECT * FROM files
            WHERE user_id = :userId
              AND parent_id IS NOT DISTINCT FROM CAST(:parentId AS UUID)
              AND is_deleted = FALSE
        """);
    }

    if (!filter.includeDirectories()) {
      sql.append(" AND is_directory = FALSE");
    }

    if (!filter.recursive()) {
      sql.append(" AND status = 'READY'");
    }

    sql.append(" ORDER BY CASE WHEN is_directory THEN 1 ELSE 2 END, name ASC");

    Query query = entityManager.createNativeQuery(sql.toString(), StorageEntity.class);
    query.setParameter("userId", filter.userId());
    query.setParameter("parentId", filter.parentId());

    return query.getResultList();
  }

  public Optional<StorageEntity> getFile(UUID fileId) {
    return jpaRepository.findByIdAndStatus(fileId, FileStatus.READY);
  }

  public Optional<StorageEntity> getFile(UUID userId, UUID parentId, String name) {
    return jpaRepository.findReadyFile(userId, parentId, name);
  }

  public Optional<StorageEntity> getFile(UUID userId, UUID fileId) {
    return jpaRepository.findByUserIdAndIdAndStatus(userId, fileId, FileStatus.READY);
  }

    public Optional<StorageEntity> getFileIncludeDeleted(UUID userId, UUID fileId) {
        return jpaRepository.findByIdIncludeDeleted(userId, fileId);
    }

  @Transactional
  public void softDeleteFile(UUID userId, UUID fileId) {
    jpaRepository.softDelete(userId, fileId);
  }

  public void hardDeleteFile(StorageEntity entity) {
    jpaRepository.deleteByUserIdAndId(entity.getUserId(), entity.getId());
  }

  @Transactional
  public void restoreFile(UUID userId, UUID fileId, boolean recursive) {
    if (recursive) {
      jpaRepository.restoreRecursive(userId, fileId);
    } else {
      jpaRepository.restore(userId, fileId);
    }
  }

  @Transactional
  public Optional<StorageEntity> getDeletedById(UUID userId, UUID fileId) {
    return jpaRepository.getDeletedById(userId, fileId);
  }

  public void updateEntity(StorageEntity entity) {
    jpaRepository.save(entity);
  }

  public boolean fileExists(UUID userId, UUID parentId, String name) {
    return fileExists(userId, parentId, name, true);
  }

  public boolean fileExists(UUID userId, UUID parentId, String name, boolean isOnlyReady) {
    return jpaRepository.existsFile(userId, parentId, name, isOnlyReady);
  }

  public boolean isDescendant(UUID sourceId, UUID targetParentId) {
    return jpaRepository.isDescendant(sourceId, targetParentId);
  }

  public long calculateTotalSizeOfTree(UUID directoryId) {
    return jpaRepository.calculateTotalSizeOfTree(directoryId);
  }
}
