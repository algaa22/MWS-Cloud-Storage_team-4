package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
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
public class ChunkedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
  private final ChunkedUploadHandler chunkedUpload;
  private final ChunkedDownloadHandler chunkedDownload;

  private static final HttpObject POISON_PILL = new DefaultHttpContent(Unpooled.EMPTY_BUFFER);
  private final BlockingQueue<HttpObject> httpObjectsQueue = new LinkedBlockingQueue<>();
  private boolean threadStarted = false;

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    clearHttpObjectsQueue();
    httpObjectsQueue.add(POISON_PILL);

    super.channelInactive(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    ReferenceCountUtil.retain(msg);
    httpObjectsQueue.add(msg);

    if (msg instanceof HttpRequest && !threadStarted) {
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
            chunkedUpload.cleanup();
            clearHttpObjectsQueue();
          }
        });
  }

  private void startHttpObjectsHandler(ChannelHandlerContext ctx) throws InterruptedException {
    while (true) {
      HttpObject msg = httpObjectsQueue.take();

      if (msg == POISON_PILL) {
        break;
      }

      try {
        if (msg instanceof HttpRequest request) {
          handleHttpRequest(ctx, request);
        } else if (msg instanceof HttpContent content) {
          handleHttpContent(ctx, content);

          if (content instanceof LastHttpContent) {
            break;
          }
        }
      } catch (Exception e) {
        ctx.executor().execute(() -> ctx.fireExceptionCaught(e));
        break;
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException,
          HeaderNotFoundException,
          TransferAlreadyStartedException {
    startChunkedTransfer(ctx, request);
  }

  private void startChunkedTransfer(ChannelHandlerContext ctx, HttpRequest request)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException,
          HeaderNotFoundException,
          TransferAlreadyStartedException,
          StorageFileNotFoundException {
    String uri = request.uri();
    HttpMethod method = request.method();

    if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
      chunkedUpload.startChunkedUpload(request);
    } else if (uri.startsWith("/api/files") && method.equals(HttpMethod.GET)) {
      chunkedDownload.startChunkedDownload(ctx, request);
    } else {
      ResponseUtils.sendMethodNotSupported(ctx, uri, method);
    }
  }

  private void handleHttpContent(ChannelHandlerContext ctx, HttpContent content)
      throws UserNotFoundException,
          StorageFileAlreadyExistsException,
          TooSmallFilePartException,
          UploadSessionNotFoundException,
          CombineChunksToPartException,
          ValidationFailedException,
          MissingFilePartException,
          TransferNotStartedYetException {
    if (content instanceof LastHttpContent) {
      chunkedUpload.completeChunkedUpload(ctx, (LastHttpContent) content);
    } else {
      chunkedUpload.handleFileChunk(content);
    }
  }

  private void clearHttpObjectsQueue() {
    HttpObject remaining;
    while ((remaining = httpObjectsQueue.poll()) != null) {
      ReferenceCountUtil.safeRelease(remaining);
    }
  }
}
