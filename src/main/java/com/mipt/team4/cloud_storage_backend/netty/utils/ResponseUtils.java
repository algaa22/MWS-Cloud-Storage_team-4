package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

public class ResponseUtils {

  public static void sendInternalServerErrorAndClose(ChannelHandlerContext ctx) {
    sendInternalServerError(ctx).addListener(ChannelFutureListener.CLOSE);
  }

  public static ChannelFuture sendInternalServerError(ChannelHandlerContext ctx) {
    return sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  public static void sendMethodNotSupported(
      ChannelHandlerContext ctx, String uri, HttpMethod method) {
    sendError(
        ctx,
        HttpResponseStatus.BAD_REQUEST,
        "Request {uri: %s, method: %s} not supported".formatted(uri, method));
  }

  public static void sendSuccess(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    send(ctx, createSuccess(status, message));
  }

  public static ChannelFuture sendError(
      ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    return send(ctx, createError(status, message));
  }

  public static void sendJson(ChannelHandlerContext ctx, HttpResponseStatus status, JsonNode json) {
    sendJson(ctx, status, json.toString());
  }

  public static void sendJson(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
    send(ctx, createJson(status, json));
  }

  private static FullHttpResponse createSuccess(HttpResponseStatus status, String message) {
    return createJson(status, true, message);
  }

  public static FullHttpResponse createError(HttpResponseStatus status, String message) {
    return createJson(status, false, message);
  }

  public static FullHttpResponse createJson(
      HttpResponseStatus status, boolean success, String message) {
    return createJson(status, createJsonNode(success, message));
  }

  public static ObjectNode createJsonNode(boolean success, String message) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    // TODO: вместо String message - JsonNode message в ошибке валидации
    json.put("success", success);
    json.put("message", message);

    return json;
  }

  public static FullHttpResponse createJson(HttpResponseStatus status, ObjectNode json) {
    return createJson(status, json.toString());
  }

  public static ChannelFuture send(ChannelHandlerContext ctx, Object response) {
    return executeWrite(ctx, response, ctx::writeAndFlush);
  }

  public static ChannelFuture write(ChannelHandlerContext ctx, Object response) {
    return executeWrite(ctx, response, ctx::write);
  }

  private static ChannelFuture executeWrite(
      ChannelHandlerContext ctx, Object response, Function<Object, ChannelFuture> operation) {
    ChannelPromise promise = ctx.newPromise();

    ctx.executor()
        .execute(
            () -> {
              try {
                if (ctx.channel().isActive()) {
                  operation
                      .apply(response)
                      .addListener(
                          future -> {
                            if (future.isSuccess()) promise.setSuccess();
                            else promise.setFailure(future.cause());
                          });
                } else {
                  ReferenceCountUtil.release(response);
                  promise.setSuccess();
                }
              } catch (Exception e) {
                ReferenceCountUtil.safeRelease(response);
                promise.setFailure(e);
                ctx.fireExceptionCaught(e);
              }
            });

    return promise;
  }

  public static FullHttpResponse createJson(HttpResponseStatus status, String json) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, StandardCharsets.UTF_8));

    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");

    return response;
  }

  public static void sendCreatedResponse(ChannelHandlerContext ctx, UUID entityId, String message) {
    ObjectNode root = ResponseUtils.createJsonNode(true, message);
    root.put("id", entityId.toString());

    ResponseUtils.sendJson(ctx, HttpResponseStatus.CREATED, root);
  }
}
