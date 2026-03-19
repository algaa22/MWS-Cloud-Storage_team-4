package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPart;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkedUploadSessionRepository extends JpaRepository<ChunkedUploadSession, UUID> {
  @Modifying
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
  void upsertPart(@Param("part") ChunkedUploadPart part);

  @Query("SELECT s FROM ChunkedUploadSession s LEFT JOIN FETCH s.parts WHERE s.id = :id")
  Optional<ChunkedUploadSession> findByIdWithParts(@Param("id") UUID id);
}
