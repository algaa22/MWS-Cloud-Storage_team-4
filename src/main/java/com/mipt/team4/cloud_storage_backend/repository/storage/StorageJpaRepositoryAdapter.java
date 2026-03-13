package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
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
public class StorageJpaRepositoryAdapter {
  private final StorageJpaRepository jpaRepository;
  private final EntityManager entityManager;

  @Transactional
  public void addFile(StorageEntity fileEntity) {
    if (fileEntity.getId() == null) fileEntity.setId(UUID.randomUUID());

    String tagsStr = FileTagsMapper.toString(fileEntity.getTags());
    jpaRepository.upsertFile(fileEntity, tagsStr);
  }

  @Transactional
  public void softDelete(UUID userId, UUID fileId, boolean isDirectory) {
    if (isDirectory) {
      jpaRepository.softDeleteRecursive(userId, fileId);
    } else {
      jpaRepository.softDelete(userId, fileId);
    }
  }

  @Transactional
  public void hardDelete(UUID userId, UUID fileId) {
    jpaRepository.deleteByUserIdAndId(userId, fileId);
  }

  @Transactional
  public void restore(UUID userId, UUID fileId, boolean isDirectory) {
    if (isDirectory) {
      jpaRepository.restoreRecursive(userId, fileId);
    } else {
      jpaRepository.restore(userId, fileId);
    }
  }

  @Transactional
  public void updateFile(StorageEntity entity) {
    jpaRepository.save(entity);
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

    if (filter.tags().isPresent()) {
      sql.append(" AND tags = :tags");
    }

    Query query = entityManager.createNativeQuery(sql.toString(), StorageEntity.class);
    sql.append(" ORDER BY CASE WHEN is_directory THEN 1 ELSE 2 END, name ASC");

    query.setParameter("userId", filter.userId());
    query.setParameter("parentId", filter.parentId());

    if (filter.tags().isPresent()) {
      query.setParameter("tags", filter.tags().get());
    }

    return query.getResultList();
  }

  @Transactional(readOnly = true)
  public Optional<StorageEntity> getDeletedById(UUID userId, UUID fileId) {
    return jpaRepository.findDeletedById(userId, fileId);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getStaleFiles(LocalDateTime threshold) {
    return jpaRepository.findByStatusInAndUpdatedAtBefore(
        List.of(FileStatus.PENDING, FileStatus.ERROR), threshold);
  }

  @Transactional(readOnly = true)
  public Optional<StorageEntity> get(UUID userId, UUID parentId, String name) {
    return jpaRepository.findByUserIdAndIdAndName(userId, parentId, name);
  }

  @Transactional(readOnly = true)
  public Optional<StorageEntity> get(UUID userId, UUID fileId) {
    return jpaRepository.findByUserIdAndId(userId, fileId);
  }

  @Transactional(readOnly = true)
  public Optional<StorageEntity> getIncludeDeleted(UUID userId, UUID fileId) {
    return jpaRepository.findByIdIncludeDeleted(userId, fileId);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getTrashFileList(UUID userId, UUID parentId) {
    return jpaRepository.findTrashByParentId(userId, parentId);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getStaleDeletedFiles(LocalDateTime threshold) {
    return jpaRepository.findStaleDeletedFiles(threshold);
  }

  @Transactional(readOnly = true)
  public boolean exists(UUID userId, UUID parentId, String name) {
    return exists(userId, parentId, name, true);
  }

  @Transactional(readOnly = true)
  public boolean exists(UUID userId, UUID parentId, String name, boolean isOnlyReady) {
    return jpaRepository.existsFile(userId, parentId, name, isOnlyReady);
  }

  @Transactional(readOnly = true)
  public boolean isDescendant(UUID sourceId, UUID targetParentId) {
    return jpaRepository.isDescendant(sourceId, targetParentId);
  }

  @Transactional(readOnly = true)
  public long calculateTotalSizeOfTree(UUID directoryId) {
    return jpaRepository.calculateTotalSizeOfTree(directoryId);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> findAllDescendants(UUID userId, UUID id) {
    return jpaRepository.findAllFilesDescendants(userId, id);
  }

  @Transactional(readOnly = true)
  public String getFullFilePath(UUID fileId) {
    List<String> nodes = jpaRepository.getFullPathNodes(fileId);

    if (nodes.isEmpty()) {
      return "";
    }

    return String.join("/", nodes);
  }
}
