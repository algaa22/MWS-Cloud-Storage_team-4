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
    @ResponseBodyParam Boolean hasPassword,
    @ResponseBodyParam Boolean isFileDeleted
) {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public static ShareInfoResponse fromShare(FileShare share, String baseUrl) {
    boolean isFileDeleted = share.getFile() == null || share.getFile().isDeleted();

    String fileName = "Файл удален";
    Long fileSize = 0L;
    String fileId = null;

    if (share.getFile() != null && !share.getFile().isDeleted()) {
      fileName = share.getFile().getName();
      fileSize = share.getFile().getSize();
      fileId = share.getFile().getId().toString();
    }

    return new ShareInfoResponse(
        share.getId().toString(),
        baseUrl + "/s?shareToken=" + share.getShareToken(),
        share.getShareToken(),
        fileId,
        fileName,
        fileSize,
        share.getShareType() != null ? share.getShareType().name() : "PUBLIC",
        share.getCreatedAt() != null ? share.getCreatedAt().format(FORMATTER) : null,
        share.getExpiresAt() != null ? share.getExpiresAt().format(FORMATTER) : null,
        share.getMaxDownloads(),
        share.getDownloadCount(),
        share.getIsActive() && !isFileDeleted,
        share.getPasswordHash() != null,
        isFileDeleted
    );
  }
}