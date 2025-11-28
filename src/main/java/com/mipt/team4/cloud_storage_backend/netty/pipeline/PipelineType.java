package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

public enum PipelineType {
  CHUNKED,
  AGGREGATED;

  public static PipelineType from(HttpRequest request) {
    if (request.method() == HttpMethod.POST) {
      String transferEncoding = RequestUtils.getHeader(request, "Transfer-Encoding", "");
      if (transferEncoding.equalsIgnoreCase("chunked")) return CHUNKED;
    }

    if (request.method() == HttpMethod.GET) {
      String downloadMode = RequestUtils.getHeader(request, "X-Download-Mode", "");
      if (downloadMode.equalsIgnoreCase("chunked")) return CHUNKED;
    }

    return AGGREGATED;
  }
}
