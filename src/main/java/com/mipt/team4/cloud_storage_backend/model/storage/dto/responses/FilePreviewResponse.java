package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.UUID;

public record FilePreviewResponse(
    @ResponseBodyParam UUID fileId,
    @ResponseBodyParam String fileName,
    @ResponseBodyParam String previewUrl,
    @ResponseBodyParam String mimeType,
    @ResponseBodyParam long fileSize,
    @ResponseBodyParam int expiresInSeconds,
    @ResponseBodyParam boolean isPreviewable) {
  private static final int PREVIEW_EXPIRY_SECONDS = 3600;

  public static FilePreviewResponse from(StorageEntity entity, String previewUrl) {
    boolean isPreviewable = previewUrl != null;

    return new FilePreviewResponse(
        entity.getId(),
        entity.getName(),
        isPreviewable ? previewUrl : null,
        entity.getMimeType(),
        entity.getSize(),
        PREVIEW_EXPIRY_SECONDS,
        isPreviewable);
  }
}
