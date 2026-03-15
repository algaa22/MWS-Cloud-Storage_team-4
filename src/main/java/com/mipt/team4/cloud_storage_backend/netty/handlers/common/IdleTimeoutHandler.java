package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Sharable
public class IdleTimeoutHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      log.warn("Idle timeout reached. Closing connection with {}", ctx.channel().remoteAddress());
      ctx.close();
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }
}
