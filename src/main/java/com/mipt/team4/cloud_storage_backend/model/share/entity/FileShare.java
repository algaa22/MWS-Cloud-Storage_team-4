package com.mipt.team4.cloud_storage_backend.model.share.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_shares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "share_token", unique = true, nullable = false)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private StorageEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "share_type")
    @Enumerated(EnumType.STRING)
    private ShareType shareType;

    public enum ShareType {
        PUBLIC,
        PROTECTED,
        PRIVATE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (downloadCount == null) downloadCount = 0;
        if (isActive == null) isActive = true;
        if (shareType == null) shareType = ShareType.PUBLIC;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean canDownload() {
        if (!isActive) return false;
        if (isExpired()) return false;
        if (maxDownloads != null && downloadCount >= maxDownloads) return false;
        return true;
    }

    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 1 : this.downloadCount + 1);
    }
}