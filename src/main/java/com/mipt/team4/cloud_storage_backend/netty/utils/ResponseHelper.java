package com.mipt.team4.cloud_storage_backend.netty.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;

public class ResponseHelper {
  public static void sendMethodNotSupportedResponse(
      String uri, HttpMethod method, ChannelHandlerContext ctx) {
    sendErrorResponse(
        ctx,
        HttpResponseStatus.BAD_REQUEST,
        "Request with uri: " + uri + ", method: " + method + " not supported");
  }

  public static ChannelFuture sendErrorResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    return sendResponse(ctx, createErrorResponse(status, message));
  }

  public static ChannelFuture sendJsonResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
    return sendResponse(ctx, createJsonResponse(status, json));
  }

  public static ChannelFuture sendResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
    return ctx.writeAndFlush(response);
  }

  public static FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
    return createJsonResponse(
        status, "{\"error\": \"" + message + "\", \"status\": " + status + "}");
  }

  public static FullHttpResponse createJsonResponse(HttpResponseStatus status, String json) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");

    return response;
  }

  public static FullHttpResponse createBinaryResponse(
      HttpResponseStatus status, byte[] data, String contentType) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(data));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);

    return response;
  }
}
