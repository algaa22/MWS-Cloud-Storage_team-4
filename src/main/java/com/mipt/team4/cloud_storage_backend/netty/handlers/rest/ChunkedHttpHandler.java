package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RouteRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
@RequiredArgsConstructor
public class ChunkedHttpHandler extends SimpleChannelInboundHandler<Object> {
  private static final HttpObject POISON_PILL = new DefaultHttpContent(Unpooled.EMPTY_BUFFER);

  private final RouteRegistry routeRegistry;
  private final RestHandlerInvoker handlerInvoker;
  private final BlockingQueue<Object> httpObjectsQueue = new LinkedBlockingQueue<>();

  private boolean threadStarted = false;

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    clearHttpObjectsQueue();
    httpObjectsQueue.add(POISON_PILL);

    super.channelInactive(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    boolean isDto = routeRegistry.isRegisteredDto(msg.getClass());

    if (msg instanceof HttpContent || isDto) {
      ReferenceCountUtil.retain(msg);
      httpObjectsQueue.add(msg);
    }

    if (isDto && !threadStarted) {
      startVirtualProcessor(ctx);
    }
  }

  private void startVirtualProcessor(ChannelHandlerContext ctx) {
    threadStarted = true;

    Thread.startVirtualThread(
        () -> {
          try {
            startHttpObjectsHandler(ctx);
          } catch (InterruptedException e) {
            throw new FatalStorageException(
                "Virtual thread of chunked http handler was interrupted", e);
          } finally {
            clearHttpObjectsQueue();
          }
        });
  }

  private void startHttpObjectsHandler(ChannelHandlerContext ctx) throws InterruptedException {
    while (true) {
      Object msg = httpObjectsQueue.take();

      if (msg == POISON_PILL) {
        break;
      }

      try {
        handlerInvoker.invoke(ctx, msg);

        if (msg instanceof LastHttpContent) {
          break;
        }
      } catch (Exception e) {
        ctx.executor().execute(() -> ctx.fireExceptionCaught(e));
        break;
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }
  }

  private void clearHttpObjectsQueue() {
    Object remaining;

    while ((remaining = httpObjectsQueue.poll()) != null) {
      ReferenceCountUtil.safeRelease(remaining);
    }
  }
}
