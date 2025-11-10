package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import io.netty.handler.codec.http.HttpRequest;

public enum PipelineType {
  CHUNKED,
  AGGREGATED;

  public static PipelineType from(HttpRequest request) {
    String transferEncoding = request.headers().get("Transfer-Encoding", "");

    if (transferEncoding.equalsIgnoreCase("chunked")) return CHUNKED;
    else return AGGREGATED;
  }
}
