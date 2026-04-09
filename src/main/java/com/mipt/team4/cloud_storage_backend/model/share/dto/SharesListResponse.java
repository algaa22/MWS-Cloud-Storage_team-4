package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.List;

public class SharesListResponse {

  @ResponseBodyParam
  private List<ShareInfoResponse> shares;

  public SharesListResponse(List<ShareInfoResponse> shares) {
    this.shares = shares;
  }

  public List<ShareInfoResponse> getShares() {
    return shares;
  }
}