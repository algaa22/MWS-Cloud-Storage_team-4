package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "chunked_upload_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkedUploadPartEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private ChunkedUploadSessionEntity session;

  @Column(name = "part_number", nullable = false)
  private Integer number;

  @Column(name = "part_size", nullable = false)
  private Long size;

  @Column(name = "etag", nullable = false)
  private String eTag;

  @Column(name = "hash")
  private String hash;
}
