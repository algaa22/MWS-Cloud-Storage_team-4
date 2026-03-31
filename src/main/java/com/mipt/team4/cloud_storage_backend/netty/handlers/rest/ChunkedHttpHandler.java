package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RoutedMessage;
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

  private final BlockingQueue<Object> httpObjectsQueue = new LinkedBlockingQueue<>();
  private final RestHandlerInvoker handlerInvoker;

  private volatile boolean threadStarted = false;
  private String currentMethod;
  private String currentPath;

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    clearHttpObjectsQueue();
    httpObjectsQueue.add(POISON_PILL);

    super.channelInactive(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpContent || msg instanceof RoutedMessage) {
      ReferenceCountUtil.retain(msg);
      httpObjectsQueue.add(msg);
    }

    if (msg instanceof RoutedMessage routedMsg && !threadStarted) {
      currentMethod = routedMsg.method();
      currentPath = routedMsg.path();

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
            threadStarted = false;
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
        handlerInvoker.invoke(ctx, msg, currentMethod, currentPath);

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
