package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.config.CorsConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class CorsHandler extends ChannelDuplexHandler {

  private static final String ALLOWED_METHODS =
      String.join(", ", "GET", "POST", "PUT", "DELETE", "OPTIONS");

  private static final String ALLOWED_HEADERS =
      String.join(
          ", ",
          HttpHeaderNames.CONTENT_TYPE.toString(),
          HttpHeaderNames.TRANSFER_ENCODING.toString(),
          "X-Auth-Email",
          "X-Auth-Password",
          "X-Auth-Token",
          "X-Auth-Username",
          "X-Download-Mode",
          "X-File-Size",
          "X-File-Tags",
          "X-File-New-Path",
          "X-File-Visibility",
          "X-New-Username",
          "X-Old-Password",
          "X-New-Password",
          "X-Refresh-Token",
          "X-File-Path",
          "X-File-New-Visibility",
          "X-File-New-Tags");

  private static final String EXPOSE_HEADERS =
      String.join(", ", "X-File-Name", "X-File-Size", "X-File-Path");

  private static final String CACHE_CONTROL =
      String.join(", ", "no-store", "no-cache", "must-revalidate");

  private static final String PRAGMA = "no-cache";

  private final CorsConfig corsConfig;

  public CorsHandler(CorsConfig corsConfig) {
    this.corsConfig = corsConfig;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest request) {
      if (request.method() == HttpMethod.OPTIONS) {
        sendPreflightResponse(ctx, request);
        return;
      }
    }

    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof HttpResponse response) {
      addCorsHeaders(response);
    }

    super.write(ctx, msg, promise);
  }

  private void sendPreflightResponse(ChannelHandlerContext ctx, HttpRequest request) {
    DefaultFullHttpResponse response =
        new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NO_CONTENT);

    addCorsHeaders(response);

    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  private void addCorsHeaders(HttpResponse response) {
    HttpHeaders headers = response.headers();

    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
    headers.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSE_HEADERS);

    headers.set(
        HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, corsConfig.accessControl().allowOrigin());
    headers.set(
        HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS,
        corsConfig.accessControl().allowCredentials());
    headers.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, corsConfig.accessControl().maxAge());

    headers.set(HttpHeaderNames.CACHE_CONTROL, CACHE_CONTROL);
    headers.set(HttpHeaderNames.PRAGMA, PRAGMA);
  }
}
