package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import java.nio.charset.StandardCharsets;

public class ResponseHelper {
  public static void sendInternalServerErrorResponse(ChannelHandlerContext ctx) {
    ResponseHelper.sendErrorResponse(
        ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  public static void sendMethodNotSupportedResponse(
      ChannelHandlerContext ctx, String uri, HttpMethod method) {
    sendErrorResponse(
        ctx,
        HttpResponseStatus.BAD_REQUEST,
        "Request with uri: " + uri + ", method: " + method + " not supported");
  }

  public static void sendValidationErrorResponse(
      ChannelHandlerContext ctx, ValidationFailedException exception) {
    sendJsonResponse(ctx, HttpResponseStatus.BAD_REQUEST, exception.toJson());
  }

  public static ChannelFuture sendSuccessResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    return sendResponse(ctx, createSuccessResponse(status, message));
  }

  public static ChannelFuture sendErrorResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    return sendResponse(ctx, createErrorResponse(status, message));
  }

  public static ChannelFuture sendJsonResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, JsonNode json) {
    return sendJsonResponse(ctx, status, json.toString());
  }

  public static ChannelFuture sendJsonResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
    return sendResponse(ctx, createJsonResponse(status, json));
  }

  public static ChannelFuture sendResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
    return ctx.writeAndFlush(response);
  }

  private static FullHttpResponse createSuccessResponse(HttpResponseStatus status, String message) {
    return createJsonResponse(status, true, message);
  }

  public static FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
    return createJsonResponse(status, false, message);
  }

  public static FullHttpResponse createJsonResponse(
      HttpResponseStatus status, boolean success, String message) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    // TODO: Json injection?
    json.put("success", success);
    json.put("message", message);
    json.put("status", status.code());

    return createJsonResponse(status, json);
  }

  public static FullHttpResponse createJsonResponse(HttpResponseStatus status, ObjectNode json) {
    return createJsonResponse(status, json.toString());
  }

  public static FullHttpResponse createJsonResponse(HttpResponseStatus status, String json) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache"); // TODO: no cache?

    return response;
  }

  public static FullHttpResponse createBinaryResponse(
      HttpResponseStatus status, byte[] data, String contentType) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(data));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.length);
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache"); // TODO: no cache?

    return response;
  }
}
