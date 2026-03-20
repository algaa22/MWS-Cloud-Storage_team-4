package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShareResponse(
    @ResponseBodyParam UUID id,
    @ResponseBodyParam String shareUrl,
    @ResponseBodyParam String shareToken,
    @ResponseBodyParam UUID fileId,
    @ResponseBodyParam String fileName,
    @ResponseBodyParam Long fileSize,
    @ResponseBodyParam FileShare.ShareType shareType,
    @ResponseBodyParam LocalDateTime createdAt,
    @ResponseBodyParam LocalDateTime expiresAt,
    @ResponseBodyParam Integer maxDownloads,
    @ResponseBodyParam Integer downloadCount,
    @ResponseBodyParam Boolean isActive,
    @ResponseBodyParam Boolean hasPassword) {
  public static ShareResponse fromShare(FileShare share, String baseUrl) {
    return new ShareResponse(
        share.getId(),
        baseUrl + "/s/" + share.getShareToken(),
        share.getShareToken(),
        share.getFile().getId(),
        share.getFile().getName(),
        share.getFile().getSize(),
        share.getShareType(),
        share.getCreatedAt(),
        share.getExpiresAt(),
        share.getMaxDownloads(),
        share.getDownloadCount(),
        share.getIsActive(),
        share.getPasswordHash() != null);
  }
}
