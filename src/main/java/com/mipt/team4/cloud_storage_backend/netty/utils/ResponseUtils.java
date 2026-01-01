package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;

public class ResponseUtils {

  public static ChannelFuture sendInternalServerErrorResponse(ChannelHandlerContext ctx) {
    // TODO: нормальная обработка внутренних ошибок
    return ResponseUtils.sendErrorResponse(
        ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  public static void sendMethodNotSupportedResponse(
      ChannelHandlerContext ctx, String uri, HttpMethod method) {
    sendErrorResponse(
        ctx,
        HttpResponseStatus.BAD_REQUEST,
        "Request {uri: %s, method: %s} not supported".formatted(uri, method));
  }

  public static void sendBadRequestExceptionResponse(
      ChannelHandlerContext ctx, Exception exception) {
    ResponseUtils.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, exception.getMessage());
  }

  public static void sendSuccessResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    sendResponse(ctx, createSuccessResponse(status, message));
  }

  public static ChannelFuture sendErrorResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    return sendResponse(ctx, createErrorResponse(status, message));
  }

  public static void sendJsonResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, JsonNode json) {
    sendJsonResponse(ctx, status, json.toString());
  }

  public static void sendJsonResponse(
      ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
    sendResponse(ctx, createJsonResponse(status, json));
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
    return createJsonResponse(status, createJsonResponseNode(success, message));
  }

  public static ObjectNode createJsonResponseNode(boolean success, String message) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    // TODO: вместо String message - JsonNode message в ошибке валидации
    json.put("success", success);
    json.put("message", message);

    return json;
  }

  public static FullHttpResponse createJsonResponse(HttpResponseStatus status, ObjectNode json) {
    return createJsonResponse(status, json.toString());
  }

  public static FullHttpResponse createJsonResponse(HttpResponseStatus status, String json) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8));

    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");

    return response;
  }
}
