package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.DownloadedChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.GetFileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleFileOperationDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
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
  private String currentFilePath; // TODO: зачем, если есть fileInfo
  private String currentUserToken;
  private long fileSize;
  private long sentBytes = 0;
  private int sentChunks = 0;
  private int totalChunks;

  public ChunkedDownloadHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void startChunkedDownload(ChannelHandlerContext ctx, HttpRequest request)
      throws TransferAlreadyStartedException,
          UserNotFoundException,
          StorageFileNotFoundException,
          StorageIllegalAccessException {
    if (isInProgress) throw new TransferAlreadyStartedException();

    FileChunkedDownloadDto fileInfo;

    try {
      parseDownloadRequestMetadata(request);
      fileInfo =
          fileController.getFileDownloadInfo(
              new SimpleFileOperationDto(currentFilePath, currentUserToken));
    } catch (ValidationFailedException | QueryParameterNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      cleanup();
      return;
    }

    isInProgress = true;
    currentSessionId = UUID.randomUUID().toString();
    totalChunks = calculateTotalChunks(fileInfo.size());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked download. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          currentSessionId,
          currentUserToken,
          currentFilePath,
          fileSize,
          totalChunks);
    }

    sendInitialHeaders(ctx, fileInfo);
    sendNextChunk(ctx);
  }

  private void sendInitialHeaders(ChannelHandlerContext ctx, FileChunkedDownloadDto fileInfo) {
    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
    response.headers().set("X-File-Path", fileInfo.path());
    response.headers().set("X-File-Size", fileInfo.size());
    // TODO: зачем sessionId? его не нужно отправлять в заголовках

    ctx.write(response);
  }

  private void sendNextChunk(ChannelHandlerContext ctx) {
    if (sentChunks >= totalChunks) {
      finishChunkedDownload(ctx);
      return;
    }

    DownloadedChunkDto fileChunk;
    int chunkIndex = sentChunks;

    try {
      fileChunk =
          fileController.getFileChunk(
              new GetFileChunkDto(
                  currentUserToken,
                  currentFilePath,
                  chunkIndex,
                  StorageConfig.INSTANCE.getFileDownloadChunkSize()));
    } catch (ValidationFailedException
        | UserNotFoundException
        | StorageFileNotFoundException
        | StorageIllegalAccessException e) {
      if (logger.isDebugEnabled())
        logger.error("Download failed, closing connection: {}", e.getMessage());

      ctx.close();
      cleanup();
      return;
    }

    HttpContent httpChunk = new DefaultHttpContent(Unpooled.copiedBuffer(fileChunk.chunkData()));

    ChannelFutureListener listener =
        createChunkSendListener(ctx, chunkIndex, fileChunk.chunkData().length);
    ctx.writeAndFlush(httpChunk).addListener(listener);
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

    // TODO: или нужно с try-with-resources?
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
    currentFilePath = null;
    currentUserToken = null;
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
              currentFilePath,
              sentChunks,
              sentBytes);
      } else {
        Throwable cause = future.cause();

        logger.error(
            "Failed to send last content for download session: {}", currentSessionId, cause);
        ResponseHelper.sendErrorResponse(
                ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage())
            .addListener(ChannelFutureListener.CLOSE);
      }
    };
  }

  private int calculateTotalChunks(long fileSize) {
    return (int) Math.ceil((double) fileSize / StorageConfig.INSTANCE.getFileDownloadChunkSize());
  }

  private void parseDownloadRequestMetadata(HttpRequest request)
      throws QueryParameterNotFoundException {
    currentFilePath = RequestUtils.getRequiredQueryParam(request, "path");
    currentUserToken = request.headers().get("X-Auth-Token", "");
  }
}
