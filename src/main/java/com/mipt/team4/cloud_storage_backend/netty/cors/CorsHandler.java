package com.mipt.team4.cloud_storage_backend.netty.cors;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders;

public class CorsHandler extends ChannelOutboundHandlerAdapter {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    if (msg instanceof HttpResponse response) {
      HttpHeaders headers = response.headers();

      headers.set("Access-Control-Allow-Origin", "*");
      headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
      headers.set("Access-Control-Allow-Headers",
          "Content-Type, X-Auth-Email, X-Auth-Password, X-Auth-Token, X-Auth-Username");
    }

    super.write(ctx, msg, promise);
  }
}

