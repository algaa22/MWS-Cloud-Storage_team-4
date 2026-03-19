package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPartEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkedUploadJpaRepository
    extends JpaRepository<ChunkedUploadSessionEntity, UUID> {
  @Modifying()
  @Query(
      value =
          """
        INSERT INTO chunked_upload_parts (id, session_id, part_number, etag, size)
        VALUES (:#{#part.id != null ? #part.id : gen_random_uuid()},
                :#{#part.session.id}, :#{#part.partNumber}, :#{#part.etag}, :#{#part.size})
        ON CONFLICT (session_id, part_number)
        DO UPDATE SET etag = EXCLUDED.etag, size = EXCLUDED.size
        """,
      nativeQuery = true)
  void upsertPart(@Param("part") ChunkedUploadPartEntity part);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM ChunkedUploadSessionEntity s WHERE s.id = :id")
  void deleteSessionById(@Param("id") UUID id);

  @Modifying
  @Query(
      "UPDATE ChunkedUploadSessionEntity s SET s.status = :newStatus "
          + "WHERE s.id = :id AND s.status = :oldStatus")
  void updateStatus(
      @Param("id") UUID id,
      @Param("oldStatus") ChunkedUploadStatus oldStatus,
      @Param("newStatus") ChunkedUploadStatus newStatus);

  @Modifying
  @Query(
      "UPDATE ChunkedUploadSessionEntity s SET s.currentSize = s.currentSize + :delta WHERE s.id = :id")
  void incrementCurrentSize(@Param("id") UUID id, @Param("size") long delta);

  @Query("SELECT s FROM ChunkedUploadSession s LEFT JOIN FETCH s.parts WHERE s.id = :id")
  Optional<ChunkedUploadSessionEntity> findByIdWithParts(@Param("id") UUID id);

  @Query(
      "SELECT COUNT(p) > 0 FROM ChunkedUploadPart p WHERE p.session.id = :sid AND p.partNumber = :pNum")
  boolean existsPart(@Param("sid") UUID sessionId, @Param("pNum") int partNumber);
}
