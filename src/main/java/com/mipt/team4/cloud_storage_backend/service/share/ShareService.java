package com.mipt.team4.cloud_storage_backend.service.share;

import com.mipt.team4.cloud_storage_backend.exception.share.*;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.share.dto.CreateShareRequest;
import com.mipt.team4.cloud_storage_backend.model.share.dto.ShareDownloadInfo;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipient;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipientId;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.share.FileShareRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.share.ShareRecipientRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final FileShareRepositoryAdapter shareRepository;
    private final ShareRecipientRepositoryAdapter recipientRepository;
    private final StorageRepository storageRepository;
    private final UserJpaRepositoryAdapter userRepository;
    private final PasswordHasher passwordHasher;

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    private static final int TOKEN_BYTES = 24;
    private static final int DEFAULT_EXPIRY_DAYS = 7;

    @Transactional
    public FileShare createShare(UUID userId, CreateShareRequest request) {
        log.info("Creating share for file: {} by user: {}", request.fileId(), userId);

        StorageEntity file = storageRepository.get(userId, request.fileId())
                .orElseThrow(() -> new StorageFileNotFoundException(request.fileId()));

        UserEntity user = userRepository.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String token = generateUniqueToken();

        FileShare share = FileShare.builder()
                .shareToken(token)
                .file(file)
                .createdBy(user)
                .shareType(request.shareType() != null ?
                        request.shareType() : FileShare.ShareType.PUBLIC)
                .maxDownloads(request.maxDownloads())
                .isActive(true)
                .build();

        String expiresAtStr = request.expiresAt();
        if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
            try {
                share.setExpiresAt(LocalDateTime.parse(expiresAtStr));
            } catch (DateTimeParseException e) {
                share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
            }
        } else {
            share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
        }

        if (request.password() != null && !request.password().isEmpty()) {
            share.setPasswordHash(passwordHasher.hash(request.password()));
            share.setShareType(FileShare.ShareType.PROTECTED);
        }


        FileShare savedShare = shareRepository.save(share);

        if (request.shareType() == FileShare.ShareType.PRIVATE
                && request.recipientUserIds() != null) {

            for (UUID recipientId : request.recipientUserIds()) {
                UserEntity recipientUser =
                        userRepository.getUserById(recipientId)
                                .orElseThrow(() -> new UserNotFoundException(recipientId));

                ShareRecipient shareRecipient =
                        ShareRecipient.builder()
                                .id(new ShareRecipientId(savedShare.getId(), recipientId))
                                .share(savedShare)
                                .user(recipientUser)
                                .permission(request.permission() != null ? request.permission() : "READ")
                                .build();

                recipientRepository.save(shareRecipient);
            }
        }

        log.info("Share created successfully with token: {}", token);
        return savedShare;
    }

    @Transactional(readOnly = true)
    public FileShare validateShare(String token, UUID userId) {
        FileShare share = shareRepository.findByShareToken(token)
                .orElseThrow(() -> new ShareNotFoundException(token));

        if (!share.getIsActive()) {
            throw new ShareExpiredException(token);
        }

        if (share.isExpired()) {
            share.setIsActive(false);
            shareRepository.save(share);
            throw new ShareExpiredException(token);
        }

        if (share.getMaxDownloads() != null &&
                share.getDownloadCount() >= share.getMaxDownloads()) {
            throw new ShareLimitExceededException(token);
        }

        if (share.getShareType() == FileShare.ShareType.PRIVATE && userId != null) {
            boolean hasAccess = recipientRepository.existsByShareIdAndUserId(share.getId(), userId);
            if (!hasAccess) {
                throw new ShareNotFoundException(token);
            }
        }

        return share;
    }

    @Transactional
    public ShareDownloadInfo prepareDownload(String token, String password) {
        FileShare share = validateShare(token, null);

        boolean requiresPassword = share.getPasswordHash() != null;
        if (requiresPassword) {
            if (password == null) {
                throw new SharePasswordRequiredException(token);
            }
            if (!passwordHasher.verify(password, share.getPasswordHash())) {
                throw new InvalidSharePasswordException();
            }
        }

        share.incrementDownloadCount();
        shareRepository.save(share);

        StorageEntity file = share.getFile();

        return ShareDownloadInfo.builder()
                .fileName(file.getName())
                .mimeType(file.getMimeType())
                .fileSize(file.getSize())
                .data(downloadFileToBytes(file))
                .requiresPassword(requiresPassword)
                .shareToken(token)
                .build();
    }

    @Transactional
    public void deactivateShare(UUID shareId, UUID userId) {
        FileShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ShareNotFoundException(shareId));

        if (!share.getCreatedBy().getId().equals(userId)) {
            throw new SecurityException("Only owner can deactivate share");
        }

        share.setIsActive(false);
        shareRepository.save(share);
        log.info("Share deactivated: {}", shareId);
    }

    @Transactional(readOnly = true)
    public List<FileShare> getUserShares(UUID userId) {
        return shareRepository.findByCreatedById(userId);
    }

    @Transactional(readOnly = true)
    public List<FileShare> getFileShares(UUID fileId, UUID userId) {
        storageRepository.get(userId, fileId)
                .orElseThrow(() -> new StorageFileNotFoundException(fileId));

        return shareRepository.findByFileId(fileId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredShares() {
        log.info("Running cleanup of expired shares");
        List<FileShare> expired = shareRepository.findExpiredShares(LocalDateTime.now());

        for (FileShare share : expired) {
            share.setIsActive(false);
            log.debug("Deactivated expired share: {}", share.getId());
            shareRepository.save(share);
        }

        log.info("Cleaned up {} expired shares", expired.size());
    }

    private String generateUniqueToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_BYTES];
        String token;

        do {
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (shareRepository.existsByShareToken(token));

        return token;
    }

    private byte[] downloadFileToBytes(StorageEntity file) {
        log.info("Downloading file to bytes: {}", file.getName());
        log.info("File size in DB: {}", file.getSize());

        try (InputStream inputStream = storageRepository.download(file)) {
            byte[] data = inputStream.readAllBytes();
            log.info("Read {} bytes from storage", data.length);
            return data;
        } catch (IOException e) {
            log.error("Failed to read file data", e);
            throw new RuntimeException("Failed to read file data", e);
        }
    }
}