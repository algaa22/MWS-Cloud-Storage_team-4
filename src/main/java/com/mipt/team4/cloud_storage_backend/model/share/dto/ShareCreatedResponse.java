package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record ShareCreatedResponse(
    @ResponseBodyParam String id,
    @ResponseBodyParam String shareUrl,
    @ResponseBodyParam String shareToken) {
  public static ShareCreatedResponse fromShare(FileShare share, String baseUrl) {
    return new ShareCreatedResponse(
        share.getId().toString(),
        baseUrl + "/s?shareToken=" + share.getShareToken(),
        share.getShareToken());
  }
}
