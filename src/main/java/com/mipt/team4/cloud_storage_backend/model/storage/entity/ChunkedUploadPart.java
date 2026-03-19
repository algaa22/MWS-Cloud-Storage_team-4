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
public class ChunkedUploadPart {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private ChunkedUploadSession session;

  @Column(name = "part_number", nullable = false)
  private Integer partNumber;

  @Column(nullable = false)
  private String eTag;

  @Column(nullable = false)
  private Long size;
}
