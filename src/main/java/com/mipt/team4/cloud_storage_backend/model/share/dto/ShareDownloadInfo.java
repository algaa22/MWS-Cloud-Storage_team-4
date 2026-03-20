package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record ShareDownloadInfo(
    @ResponseBodyParam String fileName,
    @ResponseBodyParam String mimeType,
    @ResponseBodyParam long fileSize,
    @ResponseBodyParam byte[] data,
    @ResponseBodyParam boolean requiresPassword,
    @ResponseBodyParam String shareToken) {
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String fileName;
    private String mimeType;
    private long fileSize;
    private byte[] data;
    private boolean requiresPassword;
    private String shareToken;

    public Builder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder mimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public Builder fileSize(long fileSize) {
      this.fileSize = fileSize;
      return this;
    }

    public Builder data(byte[] data) {
      this.data = data;
      return this;
    }

    public Builder requiresPassword(boolean requiresPassword) {
      this.requiresPassword = requiresPassword;
      return this;
    }

    public Builder shareToken(String shareToken) {
      this.shareToken = shareToken;
      return this;
    }

    public ShareDownloadInfo build() {
      return new ShareDownloadInfo(
          fileName, mimeType, fileSize, data, requiresPassword, shareToken);
    }
  }
}
