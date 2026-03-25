package com.mipt.team4.cloud_storage_backend.repository.storage;

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
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
        INSERT INTO chunked_upload_parts (id, session_id, part_number, part_size, etag)
        VALUES (COALESCE(:id, gen_random_uuid()), :sessionId, :partNumber, :partSize, :etag)
        ON CONFLICT (session_id, part_number)
        DO UPDATE SET etag = EXCLUDED.etag, part_size = EXCLUDED.part_size
        """,
      nativeQuery = true)
  void upsertPart(
      @Param("id") UUID id,
      @Param("sessionId") UUID sessionId,
      @Param("partNumber") int partNumber,
      @Param("partSize") long partSize,
      @Param("etag") String eTag);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM ChunkedUploadSessionEntity s WHERE s.id = :id")
  void deleteSessionById(@Param("id") UUID id);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE ChunkedUploadSessionEntity s SET s.status = :newStatus "
          + "WHERE s.id = :id AND s.status = :oldStatus")
  void updateStatus(
      @Param("id") UUID id,
      @Param("oldStatus") ChunkedUploadStatus oldStatus,
      @Param("newStatus") ChunkedUploadStatus newStatus);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE ChunkedUploadSessionEntity s SET s.currentSize = s.currentSize + :delta WHERE s.id = :id")
  void incrementCurrentSize(@Param("id") UUID id, @Param("delta") long delta);

  @Query("SELECT s FROM ChunkedUploadSessionEntity s LEFT JOIN FETCH s.parts WHERE s.id = :id")
  Optional<ChunkedUploadSessionEntity> findByIdWithParts(@Param("id") UUID id);

  @Query(
      "SELECT COUNT(p) > 0 FROM ChunkedUploadPartEntity p WHERE p.session.id = :sid AND p.number = :pNum")
  boolean existsPart(@Param("sid") UUID sessionId, @Param("pNum") int partNumber);
}
