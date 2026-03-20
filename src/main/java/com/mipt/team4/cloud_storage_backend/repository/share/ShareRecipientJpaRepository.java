package com.mipt.team4.cloud_storage_backend.repository.share;

import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipient;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipientId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShareRecipientJpaRepository extends JpaRepository<ShareRecipient, ShareRecipientId> {

    List<ShareRecipient> findByShareId(UUID shareId);

    List<ShareRecipient> findByUserId(UUID userId);

    boolean existsByShareIdAndUserId(UUID shareId, UUID userId);

    @Modifying
    void deleteByShareId(UUID shareId);

    @Modifying
    void deleteByShareIdAndUserId(UUID shareId, UUID userId);

    @Query("SELECT sr.permission FROM ShareRecipient sr WHERE sr.id.shareId = :shareId AND sr.id.userId = :userId")
    String findPermissionByShareIdAndUserId(@Param("shareId") UUID shareId, @Param("userId") UUID userId);

    @Query("SELECT sr FROM ShareRecipient sr WHERE sr.id.userId = :userId AND sr.permission = :permission")
    List<ShareRecipient> findByUserIdAndPermission(@Param("userId") UUID userId, @Param("permission") String permission);
}