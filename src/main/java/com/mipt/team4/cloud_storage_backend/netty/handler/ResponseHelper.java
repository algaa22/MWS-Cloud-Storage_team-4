package com.mipt.team4.cloud_storage_backend.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class ResponseHelper {
  public FullHttpResponse createJsonResponse(HttpResponseStatus status, String json) {
    FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
    );

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");

    return response;
  }

  public FullHttpResponse createBinaryResponse(HttpResponseStatus status, byte[] data, String contentType) {
    FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(data);
    );

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, data.length);

    return response;
  }
}
