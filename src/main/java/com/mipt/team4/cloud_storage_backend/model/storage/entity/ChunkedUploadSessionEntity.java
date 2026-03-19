package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chunked_upload_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChunkedUploadSessionEntity {
  @Id @EqualsAndHashCode.Include private UUID id;

  @Column(name = "upload_id", nullable = false)
  private String uploadId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "file_id", nullable = false)
  private StorageEntity file;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ChunkedUploadStatus status = ChunkedUploadStatus.UPLOADING;

  @Column(name = "total_parts", nullable = false)
  private int totalParts;

  @Builder.Default
  @Column(name = "current_size", nullable = false)
  private long currentSize = 0;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("number ASC")
  private List<ChunkedUploadPartEntity> parts;
}
