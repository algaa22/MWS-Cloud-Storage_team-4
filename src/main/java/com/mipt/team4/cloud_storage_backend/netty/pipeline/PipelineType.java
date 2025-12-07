package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

public enum PipelineType {
  CHUNKED,
  AGGREGATED;

  public static PipelineType from(HttpRequest request) throws ParseException {
    if (request.method() == HttpMethod.POST) {
      // TODO: или Transfer-Encoding
      int fileSize =
          SafeParser.parseInt("File size", RequestUtils.getHeader(request, "X-File-Size", "0"));

      if (fileSize > StorageConfig.INSTANCE.getMaxAggregatedContentLength()) return CHUNKED;
    }

    if (request.method() == HttpMethod.GET) {
      String downloadMode = RequestUtils.getHeader(request, "X-Download-Mode", "");
      if (downloadMode.equalsIgnoreCase("chunked")) return CHUNKED;
    }

    return AGGREGATED;
  }
}
