package com.mipt.team4.cloud_storage_backend.repository.share;

import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipient;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipientId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ShareRecipientRepositoryAdapter {

    private final ShareRecipientJpaRepository jpaRepository;

    @Transactional
    public ShareRecipient save(ShareRecipient recipient) {
        return jpaRepository.save(recipient);
    }

    @Transactional
    public void delete(ShareRecipient recipient) {
        jpaRepository.delete(recipient);
    }

    @Transactional(readOnly = true)
    public Optional<ShareRecipient> findById(ShareRecipientId id) {
        return jpaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ShareRecipient> findByShareId(UUID shareId) {
        return jpaRepository.findByShareId(shareId);
    }

    @Transactional(readOnly = true)
    public List<ShareRecipient> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByShareIdAndUserId(UUID shareId, UUID userId) {
        return jpaRepository.existsByShareIdAndUserId(shareId, userId);
    }

    @Transactional
    public void deleteByShareId(UUID shareId) {
        jpaRepository.deleteByShareId(shareId);
    }

    @Transactional
    public void deleteByShareIdAndUserId(UUID shareId, UUID userId) {
        jpaRepository.deleteByShareIdAndUserId(shareId, userId);
    }

    @Transactional(readOnly = true)
    public String findPermissionByShareIdAndUserId(UUID shareId, UUID userId) {
        return jpaRepository.findPermissionByShareIdAndUserId(shareId, userId);
    }

    @Transactional(readOnly = true)
    public List<ShareRecipient> findByUserIdAndPermission(UUID userId, String permission) {
        return jpaRepository.findByUserIdAndPermission(userId, permission);
    }
}