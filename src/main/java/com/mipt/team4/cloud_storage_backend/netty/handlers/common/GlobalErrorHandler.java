package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
public class GlobalErrorHandler extends ChannelDuplexHandler {

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    ctx.write(
        msg,
        promise.addListener(
            future -> {
              if (!future.isSuccess()) {
                exceptionCaught(ctx, future.cause());
              }
            }));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Unhandled exception in channel: {}", ctx.channel().remoteAddress(), cause);
    ResponseUtils.sendInternalServerErrorResponse(ctx).addListener(ChannelFutureListener.CLOSE);
  }
}
