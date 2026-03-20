package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.SHARES_CREATE)
public record CreateShareRequest(
    @UserId UUID userId,
    @QueryParam UUID fileId,
    @QueryParam(required = false) FileShare.ShareType shareType,
    @QueryParam(required = false) String expiresAt, // Изменено с LocalDateTime на String
    @QueryParam(required = false) Integer maxDownloads,
    @QueryParam(required = false) String password,
    @QueryParam(required = false) List<UUID> recipientUserIds,
    @QueryParam(required = false) String permission) {
  public LocalDateTime getExpiresAtAsDateTime() {
    if (expiresAt == null) return null;
    try {
      return LocalDateTime.parse(expiresAt);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public FileShare.ShareType getShareType() {
    return shareType != null ? shareType : FileShare.ShareType.PUBLIC;
  }

  public String getPermission() {
    return permission != null ? permission : "READ";
  }
}
