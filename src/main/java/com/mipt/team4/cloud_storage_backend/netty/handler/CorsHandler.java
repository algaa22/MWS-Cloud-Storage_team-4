package com.mipt.team4.cloud_storage_backend.netty.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

public class CorsHandler extends ChannelDuplexHandler {
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
    // TODO: hardcoding
    response.headers().set("Access-Control-Allow-Origin", "http://localhost:5173");
    response
        .headers()
        .set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
    response
        .headers()
        .set(
            "Access-Control-Allow-Headers",
            "Content-Type, "
                + "Transfer-Encoding, "
                + "X-Auth-Email, X-Auth-Password, X-Auth-Token, X-Auth-Username, "
                + "X-Download-Mode, X-File-Size, X-File-Tags, X-File-New-Path, X-File-Visibility, "
                + "X-New-Username, X-Old-Password, X-New-Password, "
                + "X-Refresh-Token, X-Requested-With, Authorization, "
                + "X-File-Path");
    response.headers().set("Access-Control-Allow-Credentials", "true");
    response
        .headers()
        .set(
            "Access-Control-Expose-Headers",
            "Content-Disposition, X-File-Name, X-File-Size, X-File-Path");
    response.headers().set("Access-Control-Max-Age", "3600");
  }
}
