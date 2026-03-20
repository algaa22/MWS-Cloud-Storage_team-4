package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.time.format.DateTimeFormatter;

public record ShareInfoResponse(
    @ResponseBodyParam String id,
    @ResponseBodyParam String shareUrl,
    @ResponseBodyParam String shareToken,
    @ResponseBodyParam String fileId,
    @ResponseBodyParam String fileName,
    @ResponseBodyParam Long fileSize,
    @ResponseBodyParam String shareType,
    @ResponseBodyParam String createdAt,
    @ResponseBodyParam String expiresAt,
    @ResponseBodyParam Integer maxDownloads,
    @ResponseBodyParam Integer downloadCount,
    @ResponseBodyParam Boolean isActive,
    @ResponseBodyParam Boolean hasPassword) {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public static ShareInfoResponse fromShare(FileShare share, String baseUrl) {
    return new ShareInfoResponse(
        share.getId().toString(),
        baseUrl + "/s/" + share.getShareToken(),
        share.getShareToken(),
        share.getFile().getId().toString(),
        share.getFile().getName(),
        share.getFile().getSize(),
        share.getShareType() != null ? share.getShareType().name() : "PUBLIC",
        share.getCreatedAt() != null ? share.getCreatedAt().format(FORMATTER) : null,
        share.getExpiresAt() != null ? share.getExpiresAt().format(FORMATTER) : null,
        share.getMaxDownloads(),
        share.getDownloadCount(),
        share.getIsActive(),
        share.getPasswordHash() != null);
  }
}
