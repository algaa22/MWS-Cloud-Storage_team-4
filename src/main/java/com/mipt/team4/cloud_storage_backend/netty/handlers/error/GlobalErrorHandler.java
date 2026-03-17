package com.mipt.team4.cloud_storage_backend.netty.handlers.error;

import com.mipt.team4.cloud_storage_backend.netty.constants.NettyAttributes;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Sharable
@Slf4j
public class GlobalErrorHandler extends ChannelDuplexHandler {
  private static final String FATAL_ERROR_JSON =
      "{\"success\":false,\"message\":\"Internal Server Error\"}";

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    super.write(
        ctx,
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
    logError(ctx, cause);

    if (ctx.channel().isActive() && ctx.channel().isOpen()) {
      sendRawInternalErrorAndClose(ctx);
    } else {
      ctx.close();
    }
  }

  private void logError(ChannelHandlerContext ctx, Throwable cause) {
    SocketAddress remoteAddress = ctx.channel().remoteAddress();

    if (isIgnorableException(cause)) {
      if (!ctx.channel().hasAttr(NettyAttributes.IGNORABLE_ERROR_LOGGED)) {
        log.debug("Connection closed by client: {}", remoteAddress, cause);
        ctx.channel().attr(NettyAttributes.IGNORABLE_ERROR_LOGGED).set(true);
      }
    } else {
      log.error("Unhandled exception in channel: {}", remoteAddress, cause);
    }
  }

  private void sendRawInternalErrorAndClose(ChannelHandlerContext ctx) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            Unpooled.copiedBuffer(FATAL_ERROR_JSON, CharsetUtil.UTF_8));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

    ResponseUtils.send(ctx, response).addListener(ChannelFutureListener.CLOSE);
  }

  private boolean isIgnorableException(Throwable cause) {
    String message = cause.getMessage();

    return message != null
        && (message.contains("Connection reset")
            || message.contains("Broken pipe")
            || message.contains("Connection refused"));
  }
}
