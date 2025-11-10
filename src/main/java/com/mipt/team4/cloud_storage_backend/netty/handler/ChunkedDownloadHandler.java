package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.http.validation.FileDownloadValidationException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfo;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedDownloadHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChunkedDownloadHandler.class);
  private final FileController fileController;

  private boolean isInProgress = false;
  private String currentSessionId;
  private String currentFileId;
  private String currentUserId;
  private long fileSize;
  private long sentBytes = 0;
  private long sentChunks = 0;
  private int totalChunks;

  public ChunkedDownloadHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void startChunkedDownload(ChannelHandlerContext ctx, HttpRequest request)
      throws TransferAlreadyStartedException {
    if (isInProgress) throw new TransferAlreadyStartedException();

    parseDownloadRequestMetadata(request);

    FileDownloadInfo fileInfo;

    try {
      fileInfo = fileController.getFileDownloadInfo(currentFileId, currentUserId);
    } catch (FileDownloadValidationException e) {
      // TODO: error
      return;
    }

    isInProgress = true;
    currentSessionId = UUID.randomUUID().toString();
    totalChunks = calculateTotalChunks(fileInfo.size());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked download. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          currentSessionId,
          currentUserId,
          currentFileId,
          fileSize,
          totalChunks);
    }

    sendDownloadStartResponse(ctx, fileInfo);
    sendNextChunk(ctx);
  }

  private void sendNextChunk(ChannelHandlerContext ctx) {
    if (sentChunks >= totalChunks) {
      finishChunkedDownload(ctx);
      return;
    }

    int chunkIndex = totalChunks;
    int maxChunkSize = StorageConfig.getInstance().getFileDownloadChunkSize();
    long offset = (long) chunkIndex * maxChunkSize;
    int chunkSize = (int) Math.min(maxChunkSize, fileSize - offset);

    FileChunk fileChunk = fileController.getFileChunk(currentFileId, chunkIndex, chunkSize);
    HttpContent httpChunk = new DefaultHttpContent(Unpooled.copiedBuffer(fileChunk.chunkData()));

    ChannelFutureListener listener = createChunkSendListener(ctx, chunkIndex, chunkSize);
    ctx.write(httpChunk).addListener(listener);
  }

  private ChannelFutureListener createChunkSendListener(
      ChannelHandlerContext ctx, int chunkIndex, int chunkSize) {
    return future -> {
      if (future.isSuccess()) {
        handleChunkSendSuccess(ctx, chunkSize);
      } else {
        handleChunkSendFailure(ctx, chunkIndex, future.cause());
      }
    };
  }

  private void handleChunkSendSuccess(ChannelHandlerContext ctx, int chunkSize) {
    sentChunks++;
    sentBytes += chunkSize;

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Sent chunk {}/{} for session: {}, size: {}",
          sentChunks,
          totalChunks,
          currentSessionId,
          chunkSize);
    }

    ctx.channel().eventLoop().execute(() -> sendNextChunk(ctx));
  }

  private void finishChunkedDownload(ChannelHandlerContext ctx) {
    LastHttpContent lastContent = LastHttpContent.EMPTY_LAST_CONTENT;

    ChannelFutureListener listener = createLastContentSendListener(ctx);
    ctx.writeAndFlush(lastContent).addListener(listener);

    cleanup();
  }

  public void cleanup() {
    isInProgress = false;
    currentSessionId = null;
    currentFileId = null;
    currentUserId = null;
    fileSize = 0;
    sentBytes = 0;
    sentChunks = 0;
    totalChunks = 0;
  }

  private void handleChunkSendFailure(ChannelHandlerContext ctx, int chunkIndex, Throwable cause) {
    logger.error("Failed to send chunk {} for session: {}", chunkIndex, currentSessionId);
    ResponseHelper.sendErrorResponse(
            ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage())
        .addListener(ChannelFutureListener.CLOSE);
  }

  private ChannelFutureListener createLastContentSendListener(ChannelHandlerContext ctx) {
    return future -> {
      if (future.isSuccess()) {
        if (logger.isDebugEnabled())
          logger.debug(
              "Completed chunked download. Session: {}, file: {}, chunks: {}, bytes: {}",
              currentSessionId,
              currentFileId,
              totalChunks,
              sentChunks);
      } else {
        Throwable cause = future.cause();

        logger.error(
            "Failed to send last content for download session: {}", currentSessionId, cause);
        ResponseHelper.sendErrorResponse(
                ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage())
            .addListener(ChannelFutureListener.CLOSE);
      }
      // TODO: reset state?
    };
  }

  private int calculateTotalChunks(long fileSize) {
    return (int)
        Math.ceil((double) fileSize / StorageConfig.getInstance().getFileDownloadChunkSize());
  }

  private void parseDownloadRequestMetadata(HttpRequest request) {
    String uri = request.uri();
    String[] uriParts = uri.split("/");

    currentFileId = uriParts[uriParts.length - 1];
    // TODO: аутентификация
    currentUserId = request.headers().get("X-User-Id", "");
  }

  private void sendDownloadStartResponse(ChannelHandlerContext ctx, FileDownloadInfo fileInfo) {
    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
    response.headers().set("X-File-Path", fileInfo.path());
    response.headers().set("X-File-Size", fileInfo.size());
    response.headers().set("X-File-Type", fileInfo.type());
    response.headers().set("X-Total-Chunks", totalChunks);
    response.headers().set("X-File-Path", fileInfo.path());
    response.headers().set("X-Session-Id", currentSessionId);

    ctx.writeAndFlush(response);
  }
}
