package com.mipt.team4.cloud_storage_backend.model.share.entity;

import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "share_recipients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRecipient {

    @EmbeddedId
    private ShareRecipientId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", referencedColumnName = "id", insertable = false, updatable = false)
    private FileShare share;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private UserEntity user;

    @Column(name = "permission")
    private String permission;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}