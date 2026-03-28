package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.common.dto.PageQuery;
import com.mipt.team4.cloud_storage_backend.model.common.mappers.PaginationMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StorageJpaRepositoryAdapter {
  private final StorageJpaRepository jpaRepository;
  private final EntityManager entityManager;

  private static final String RECURSIVE_SEARCH =
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
        """;

  private static final String SIMPLE_SEARCH =
      """
            SELECT * FROM files
            WHERE user_id = :userId
              AND parent_id IS NOT DISTINCT FROM CAST(:parentId AS UUID)
              AND is_deleted = FALSE
        """;

  @Transactional
  public void addFile(StorageEntity fileEntity) {
    if (fileEntity.getId() == null) fileEntity.setId(UUID.randomUUID());

    jpaRepository.upsertFile(fileEntity);
    syncTags(fileEntity.getId(), fileEntity.getTags());
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
  public void syncLifecycleMetadata(
      UUID fileId, FileStatus status, int retryCount, FileOperationType opType) {
    jpaRepository.syncLifecycleMetadata(fileId, status, retryCount, opType);
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getFileList(FileListFilter filter, PageQuery pageQuery) {
    QueryContext ctx = buildBaseQueryWithFilters(filter);
    long total = fetchTotalCount(ctx);
    List<StorageEntity> content = fetchPageContent(ctx, pageQuery);

    return new PageImpl<>(
        content, PageRequest.of(pageQuery.offset() / pageQuery.limit(), pageQuery.limit()), total);
  }

  @Transactional(readOnly = true)
  public Optional<StorageEntity> getDeleted(UUID userId, UUID fileId) {
    return jpaRepository.findDeletedById(userId, fileId);
  }

  @Transactional(readOnly = true)
  public Slice<StorageEntity> getStaleFiles(LocalDateTime threshold, Pageable pageable) {
    return jpaRepository.findByStatusInAndUpdatedAtBefore(
        List.of(FileStatus.PENDING, FileStatus.ERROR), threshold, pageable);
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
  public Optional<StorageEntity> getIncludeDeleted(UUID userId, UUID parentId, String name) {
    return jpaRepository.findByParentIdAndNameIncludeDeleted(userId, parentId, name);
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getTrashFileList(UUID userId, UUID parentId, PageQuery pageQuery) {
    return jpaRepository.findTrashByParentId(
        userId, parentId, PaginationMapper.toPageable(pageQuery));
  }

  @Transactional(readOnly = true)
  public Slice<StorageEntity> getStaleDeletedFiles(LocalDateTime threshold, Pageable pageable) {
    return jpaRepository.findStaleDeletedFiles(threshold, pageable);
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

  private void syncTags(UUID fileId, List<String> tags) {
    entityManager
        .createNativeQuery("DELETE FROM file_tags WHERE file_id = :fileId")
        .setParameter("fileId", fileId)
        .executeUpdate();

    if (tags != null && !tags.isEmpty()) {
      String sql = "INSERT INTO file_tags (file_id, tag) " + "SELECT :fileId, unnest(:tags)";

      entityManager
          .createNativeQuery(sql)
          .setParameter("fileId", fileId)
          .setParameter("tags", tags.toArray(new String[0]))
          .executeUpdate();
    }
  }

  private QueryContext buildBaseQueryWithFilters(FileListFilter filter) {
    String baseSql = filter.recursive() ? RECURSIVE_SEARCH : SIMPLE_SEARCH;
    StringBuilder sql = new StringBuilder(baseSql);
    Map<String, Object> params = new HashMap<>();

    params.put("userId", filter.userId());
    params.put("parentId", filter.parentId());

    if (!filter.includeDirectories()) {
      sql.append(" AND is_directory = FALSE");
    }

    if (!filter.recursive()) {
      sql.append(" AND status = 'READY'");
    }

    if (filter.tags() != null && !filter.tags().isEmpty()) {
      sql.append(
          """
             AND EXISTS (
                SELECT 1 FROM file_tags ft
                WHERE ft.file_id = id AND ft.tag IN (:tags)
                GROUP BY ft.file_id HAVING COUNT(DISTINCT ft.tag) = :tagCount
             )
        """);
      params.put("tags", filter.tags());
      params.put("tagCount", filter.tags().size());
    }

    return new QueryContext(sql, params);
  }

  private long fetchTotalCount(QueryContext ctx) {
    String countSql = "SELECT COUNT(*) FROM (" + ctx.sql.toString() + ") as t";
    Query query = entityManager.createNativeQuery(countSql);
    ctx.params().forEach(query::setParameter);

    return ((Number) query.getSingleResult()).longValue();
  }

  @SuppressWarnings("unchecked")
  private List<StorageEntity> fetchPageContent(QueryContext ctx, PageQuery pageQuery) {
    String finalSql =
        ctx.sql()
            + " ORDER BY is_directory DESC, %s %s"
                .formatted(pageQuery.order(), pageQuery.direction())
            + " LIMIT :limit OFFSET :offset";

    Query query = entityManager.createNativeQuery(finalSql, StorageEntity.class);
    ctx.params().forEach(query::setParameter);

    query.setParameter("limit", pageQuery.limit());
    query.setParameter("offset", pageQuery.offset());

    return query.getResultList();
  }

  private record QueryContext(StringBuilder sql, Map<String, Object> params) {}
}
