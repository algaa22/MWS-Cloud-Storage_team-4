package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.SHARES_CREATE)
public record CreateShareRequest(
    @UserId UUID userId,
    @QueryParam UUID fileId,
    @QueryParam(required = false) FileShare.ShareType shareType,
    @QueryParam(required = false) String expiresAt,
    @QueryParam(required = false) Integer maxDownloads,
    @QueryParam(required = false) String password,
    @QueryParam(required = false) List<UUID> recipientUserIds,
    @QueryParam(required = false) String permission) {

  public String getPermission() {
    return permission != null ? permission : "READ";
  }
}
