package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class AggregatedHttpHandler extends SimpleChannelInboundHandler<Object> {
  private final RestHandlerInvoker handlerInvoker;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    ReferenceCountUtil.retain(msg);
    startVirtualProcessor(ctx, msg);
  }

  private void startVirtualProcessor(ChannelHandlerContext ctx, Object msg) {
    Thread.startVirtualThread( // TODO: interrupted exception
        () -> {
          try {
            handlerInvoker.invoke(ctx, msg);
          } catch (Exception e) {
            ctx.executor().execute(() -> ctx.fireExceptionCaught(e));
          } finally {
            ReferenceCountUtil.release(msg);
          }
        });
  }
}
