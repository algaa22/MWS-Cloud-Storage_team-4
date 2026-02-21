package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import java.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
public class GlobalErrorHandler extends ChannelDuplexHandler {
  private static final AttributeKey<Boolean> IGNORABLE_ERROR_LOGGED =
      AttributeKey.valueOf("ignorable_error_logged");

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
    SocketAddress remoteAddress = ctx.channel().remoteAddress();

    if (isIgnorableException(cause)) {
      if (!ctx.channel().hasAttr(IGNORABLE_ERROR_LOGGED)) {
        log.debug("Connection closed by client: {}", remoteAddress, cause);
        ctx.channel().attr(IGNORABLE_ERROR_LOGGED).set(true);
      }
    } else {
      log.error("Unhandled exception in channel: {}", remoteAddress, cause);
    }

    if (ctx.channel().isActive() && ctx.channel().isOpen()) {
      ResponseUtils.sendInternalServerErrorResponse(ctx).addListener(ChannelFutureListener.CLOSE);
    } else {
      ctx.close();
    }
  }

  private boolean isIgnorableException(Throwable cause) {
    String message = cause.getMessage();

    return message != null
        && (message.contains("Connection reset")
            || message.contains("Broken pipe")
            || message.contains("Connection refused"));
  }
}
