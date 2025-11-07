package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileDownloadValidateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileUploadValidateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.util.ArrayList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
  private static final Logger logger = LoggerFactory.getLogger(ChunkedHttpHandler.class);
  private final int SEND_UPLOAD_PROGRESS_INTERVAL = 10;
  private final FileController fileController;
  private final UserController userController;

  private static class ChunkedUploadInfo {
    public boolean isInProgress = false;
    public String currentSessionId;
    public String currentUserId;
    public String currentFilePath;
    public long totalFileSize;
    public long receivedBytes = 0;
    public int receivedChunks = 0;
    public int totalChunks;
  }

  private static class ChunkedDownloadInfo {
    public boolean isInProgress = false;
    public String currentSessionId;
    public String currentFileId;
    public String currentUserId;
    public long fileSize;
    public long sentBytes = 0;
    public long sentChunks = 0;
    public int totalChunks;
  }

  ChunkedUploadInfo chunkedUpload;
  ChunkedDownloadInfo chunkedDownload;

  public ChunkedHttpHandler(FileController fileController, UserController userController) {
    this.fileController = fileController;
    this.userController = userController;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof HttpRequest request) {
      handleHttpRequest(ctx, request);
    } else if (msg instanceof HttpContent content) {
      handleHttpContent(ctx, content);
    }

    // TODO: errors
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
    if (chunkedUpload.isInProgress) {
      handleBadRequest(
          ctx,
          "New HttpRequest received while previous request is in progress",
          "Previous request not completed");
      return;
    }

    chunkedUpload.isInProgress = true;

    startChunkedTransfer(ctx, request);
  }

  private void startChunkedTransfer(ChannelHandlerContext ctx, HttpRequest request) {
    String uri = request.uri();
    HttpMethod method = request.method();

    if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
      startChunkedUpload(ctx, request);
    } else if (uri.startsWith("/api/files/") && method.equals(HttpMethod.GET)) {
      startChunkedDownload(ctx, request);
    } else {
      ResponseHelper.sendBadRequestResponse(uri, method, ctx);
    }
  }

  private void handleHttpContent(ChannelHandlerContext ctx, HttpContent content) {
    if (!chunkedUpload.isInProgress) {
      handleTransferNotStartedYet(ctx);
      return;
    }

    if (content instanceof LastHttpContent) {
      finishChunkedUpload(ctx, (LastHttpContent) content);
    } else {
      handleFileChunk(ctx, content);
    }
  }

  public void startChunkedUpload(ChannelHandlerContext ctx, HttpRequest request) {
    parseUploadRequestMetadata(request);

    chunkedUpload.isInProgress = true;
    chunkedUpload.receivedChunks = 0;
    chunkedUpload.receivedBytes = 0;

    try {
      // TODO: получение тегов
      fileController.startChunkedUpload(
          new FileChunkedUploadSession(
              chunkedUpload.currentSessionId,
              chunkedUpload.currentUserId,
              chunkedUpload.totalFileSize,
              chunkedUpload.totalChunks,
              chunkedUpload.currentFilePath,
              null,
              new ArrayList<>()));
    } catch (FileUploadValidateException e) {
      // TODO: ошибка валидации
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked upload. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          chunkedUpload.currentSessionId,
          chunkedUpload.currentUserId,
          chunkedUpload.currentFilePath,
          chunkedUpload.totalFileSize,
          chunkedUpload.totalChunks);
    }

    sendProgressResponse(ctx, "upload_started");
  }

  private void startChunkedDownload(ChannelHandlerContext ctx, HttpRequest request) {
    if (!chunkedDownload.isInProgress) {
      handleTransferNotStartedYet(ctx);
      return;
    }

    parseDownloadRequestMetadata(request);

    FileDownloadInfo fileInfo;

    try {
      fileInfo =
          fileController.getFileDownloadInfo(
              chunkedDownload.currentFileId, chunkedDownload.currentUserId);
    } catch (FileDownloadValidateException e) {
      // TODO: error
      return;
    }

    chunkedDownload.isInProgress = true;
    chunkedDownload.currentSessionId = UUID.randomUUID().toString();
    chunkedDownload.totalChunks = calculateTotalChunks(fileInfo.size());

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked download. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          chunkedDownload.currentSessionId,
          chunkedDownload.currentUserId,
          chunkedDownload.currentFileId,
          chunkedDownload.fileSize,
          chunkedDownload.totalChunks);
    }

    sendDownloadStartResponse(ctx, fileInfo);
    sendNextChunk(ctx);
  }

  private void sendNextChunk(ChannelHandlerContext ctx) {
    if (chunkedDownload.sentChunks >= chunkedDownload.totalChunks) {
      finishChunkedDownload(ctx);
      return;
    }

    int chunkIndex = chunkedDownload.totalChunks;
    int maxChunkSize = StorageConfig.getInstance().getFileDownloadChunkSize();
    long offset = (long) chunkIndex * maxChunkSize;
    int chunkSize = (int) Math.min(maxChunkSize, chunkedDownload.fileSize - offset);

    FileChunk fileChunk =
        fileController.getFileChunk(chunkedDownload.currentFileId, chunkIndex, chunkSize);
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
    chunkedDownload.sentChunks++;
    chunkedDownload.sentBytes += chunkSize;

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Sent chunk {}/{} for session: {}, size: {}",
          chunkedDownload.sentChunks,
          chunkedDownload.totalChunks,
          chunkedDownload.currentSessionId,
          chunkSize);
    }

    ctx.channel().eventLoop().execute(() -> sendNextChunk(ctx));
  }

  private void finishChunkedDownload(ChannelHandlerContext ctx) {
    LastHttpContent lastContent = LastHttpContent.EMPTY_LAST_CONTENT;

    ChannelFutureListener listener = createLastContentSendListener(ctx);
    ctx.writeAndFlush(lastContent).addListener(listener);
  }

  private void handleChunkSendFailure(ChannelHandlerContext ctx, int chunkIndex, Throwable cause) {
    logger.error(
        "Failed to send chunk {} for session: {}", chunkIndex, chunkedDownload.currentSessionId);
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
              chunkedDownload.currentSessionId,
              chunkedDownload.currentFileId,
              chunkedDownload.totalChunks,
              chunkedDownload.sentChunks);
      } else {
        Throwable cause = future.cause();

        logger.error(
            "Failed to send last content for download session: {}",
            chunkedDownload.currentSessionId,
            cause);
        ResponseHelper.sendErrorResponse(
                ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage())
            .addListener(ChannelFutureListener.CLOSE);
      }
      // TODO: reset state?
    };
  }

  private void finishChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content) {
    if (content.content().readableBytes() > 0) handleFileChunk(ctx, content);

    String fileId = fileController.finishChunkedUpload(chunkedUpload.currentSessionId);

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, fileId: {}, chunks: {}, bytes: {}",
          fileId,
          chunkedUpload.currentSessionId,
          chunkedUpload.receivedChunks,
          chunkedUpload.receivedBytes);

    sendSuccessResponse(ctx, fileId, chunkedUpload.totalFileSize);
    chunkedUpload.isInProgress = false;
  }

  private void handleFileChunk(ChannelHandlerContext ctx, HttpContent content) {
    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    chunkedUpload.receivedChunks++;
    chunkedUpload.receivedBytes += chunkSize;

    try {
      fileController.processFileChunk(
          new FileChunk(
              chunkedUpload.currentSessionId,
              chunkedUpload.currentFilePath,
              chunkedUpload.receivedChunks,
              chunkBytes));
    } catch (FileUploadValidateException e) {
      // TODO
      return;
    }

    logger.debug(
        "Processed chunk {}/{} for session: {}. Size: {} bytes, total: {} bytes",
        chunkedUpload.receivedChunks,
        chunkedUpload.totalChunks,
        chunkedUpload.currentSessionId,
        chunkSize,
        chunkedUpload.totalFileSize);

    if (chunkedUpload.receivedChunks % SEND_UPLOAD_PROGRESS_INTERVAL == 0
        || chunkedUpload.receivedChunks == chunkedUpload.totalChunks)
      sendProgressResponse(ctx, "upload_progress");
  }

  private void parseUploadRequestMetadata(HttpRequest request) {
    // TODO: парсинг метаданных, аутентификация
    chunkedUpload.currentSessionId = "";
    chunkedUpload.currentUserId = "";
    chunkedUpload.currentFilePath = "";
    chunkedUpload.totalFileSize = Long.parseLong(request.headers().get("X-File-Size"));
    chunkedUpload.totalChunks = Integer.parseInt(request.headers().get("X-Total-Chunks"));
  }

  private int calculateTotalChunks(long fileSize) {
    return (int)
        Math.ceil((double) fileSize / StorageConfig.getInstance().getFileDownloadChunkSize());
  }

  private void parseDownloadRequestMetadata(HttpRequest request) {
    String uri = request.uri();
    String[] uriParts = uri.split("/");

    chunkedDownload.currentFileId = uriParts[uriParts.length - 1];
    // TODO: аутентификация
    chunkedDownload.currentUserId = request.headers().get("X-User-Id", "");
  }

  private void handleTransferNotStartedYet(ChannelHandlerContext ctx) {
    handleBadRequest(
        ctx, "HttpContent received without active HttpRequest", "HTTP content without request");
  }

  private void sendDownloadStartResponse(ChannelHandlerContext ctx, FileDownloadInfo fileInfo) {
    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
    response.headers().set("X-File-Path", fileInfo.path());
    response.headers().set("X-File-Size", fileInfo.size());
    response.headers().set("X-File-Type", fileInfo.type());
    response.headers().set("X-Total-Chunks", chunkedDownload.totalChunks);
    response.headers().set("X-File-Path", fileInfo.path());
    response.headers().set("X-Session-Id", chunkedDownload.currentSessionId);

    ctx.writeAndFlush(response);
  }

  private void sendProgressResponse(ChannelHandlerContext ctx, String status) {
    String json =
        String.format(
            "{\"status\":\"%s\",\"currentChunk\":%d,\"totalChunks\":%d,\"bytesReceived\":%d,\"sessionId\":\"%s\"}",
            status,
            chunkedUpload.receivedChunks,
            chunkedUpload.totalChunks,
            chunkedUpload.receivedBytes,
            chunkedUpload.currentSessionId);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, String fileId, long totalFileSize) {
    String json =
        String.format(
            "{\"status\":\"complete\",\"fileId\":%s,\"fileSize\":%d}", fileId, totalFileSize);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void handleBadRequest(
      ChannelHandlerContext ctx, String loggerMessage, String responseMessage) {
    logger.error(loggerMessage);

    ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, responseMessage)
        .addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unhandled exception in channel from {}", ctx.channel().remoteAddress(), cause);

    ResponseHelper.sendErrorResponse(
            ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error")
        .addListener(ChannelFutureListener.CLOSE);

    chunkedUpload.isInProgress = false;
  }
}
