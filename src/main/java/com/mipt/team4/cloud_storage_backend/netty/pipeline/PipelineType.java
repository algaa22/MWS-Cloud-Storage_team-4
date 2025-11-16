package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import io.netty.handler.codec.http.HttpRequest;

public enum PipelineType {
  CHUNKED,
  AGGREGATED;

  public static PipelineType from(HttpRequest request) {
    String transferEncoding = RequestUtils.getHeader(request, "Transfer-Encoding", "");

    if (transferEncoding.equalsIgnoreCase("chunked")) return CHUNKED;
    else return AGGREGATED;
  }
}
