package com.mipt.team4.cloud_storage_backend.netty.cors;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

public class CompleteCorsHandler extends ChannelDuplexHandler {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest request) {
      // Если это OPTIONS запрос (preflight) - сразу отвечаем
      if (request.method() == HttpMethod.OPTIONS) {
        sendPreflightResponse(ctx, request);
        return;
      }
    }

    // Для обычных запросов - пропускаем дальше
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    // КО ВСЕМ ответам добавляем CORS заголовки
    if (msg instanceof HttpResponse response) {
      addCorsHeaders(response);
    }

    super.write(ctx, msg, promise);
  }

  private void sendPreflightResponse(ChannelHandlerContext ctx, HttpRequest request) {
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK
    );

    addCorsHeaders(response);
    response.headers().set("Content-Type", "text/plain");
    response.headers().set("Content-Length", "0");

    ctx.writeAndFlush(response);
  }

  private void addCorsHeaders(HttpResponse response) {
    response.headers().set("Access-Control-Allow-Origin", "http://localhost:5173");
    response.headers().set("Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS, PATCH");
    response.headers().set("Access-Control-Allow-Headers",
        "Content-Type, " +
            "X-Auth-Email, X-Auth-Password, X-Auth-Token, X-Auth-Username, " +
            "X-File-Tags, X-File-New-Path, X-File-Visibility, " +
            "X-New-Username, X-Old-Password, X-New-Password, " +
            "X-Requested-With, Authorization");
    response.headers().set("Access-Control-Allow-Credentials", "true");
    response.headers().set("Access-Control-Expose-Headers",
        "Content-Disposition, X-File-Name, X-File-Size");
    response.headers().set("Access-Control-Max-Age", "3600");
  }
}