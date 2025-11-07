package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileUploadValidateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfo;
import io.netty.buffer.ByteBuf;
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

  private class ChunkedUploadInfo {
    public boolean isInProgress = false;
    public String currentSessionId;
    public String currentUserId;
    public String currentFilePath;
    public long totalFileSize;
    public long receivedBytes = 0;
    public int receivedChunks = 0;
    public int totalChunks;
  }
  ;

  private class ChunkedDownloadInfo {
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
      logger.info(
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

    FileDownloadInfo fileInfo =
        fileController.getFileDownloadInfo(
            chunkedDownload.currentFileId, chunkedDownload.currentUserId);
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
          new FileChunk(chunkedUpload.currentSessionId, chunkedUpload.receivedChunks, chunkBytes));
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
    chunkedUpload.totalFileSize = Long.parseLong(request.headers().get("X-Total-File-Size", "0"));
    chunkedUpload.totalChunks = Integer.parseInt(request.headers().get("X-Total-Chunks", "1"));
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
