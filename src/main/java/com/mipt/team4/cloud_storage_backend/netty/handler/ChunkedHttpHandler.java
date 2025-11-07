package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileUploadValidateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
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

  private boolean requestInProgress = false;
  private String currentSessionId;
  private String currentUserId;
  private String currentFilePath;
  private long totalFileSize;
  private long receivedBytes = 0;
  private int receivedChunks = 0;
  private int totalChunks;

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
    if (requestInProgress) {
      handleBadRequest(
          ctx,
          "New HttpRequest received while previous request is in progress",
          "Previous request not completed");
      return;
    }

    requestInProgress = true;

    startChunkedUpload(ctx, request);
  }

  private void handleHttpContent(ChannelHandlerContext ctx, HttpContent content) {
    if (!requestInProgress) {
      handleBadRequest(
          ctx, "HttpContent received without active HttpRequest", "HTTP content without request");
      return;
    }

    if (content instanceof LastHttpContent) {
      finishChunkedUpload(ctx, (LastHttpContent) content);
    } else {
      handleFileChunk(ctx, content);
    }
  }

  private void startChunkedTransfer(ChannelHandlerContext ctx, HttpRequest request) {
    String uri = request.uri();
    HttpMethod method = request.method();

    if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
      startChunkedUpload(ctx, request);
    } else if (uri.startsWith("/api/files/") && method.equals(HttpMethod.GET)) {
      // TODO
    } else {
      ResponseHelper.sendBadRequestResponse(uri, method, ctx);
    }
  }

  public void startChunkedUpload(ChannelHandlerContext ctx, HttpRequest request) {
    parseRequestMetadata(request);

    requestInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

    try {
      // TODO: получение тегов
      fileController.startChunkedUpload(
          new FileChunkedUploadSession(
              currentSessionId,
              currentUserId,
              totalFileSize,
              totalChunks,
              currentFilePath,
              null,
              new ArrayList<>()));
    } catch (FileUploadValidateException e) {
      // TODO: ошибка валидации
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.info(
          "Started chunked upload. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          currentSessionId,
          currentUserId,
          currentFilePath,
          totalFileSize,
          totalChunks);
    }

    sendProgressResponse(ctx, "upload_started");
  }

  private void finishChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content) {
    if (content.content().readableBytes() > 0) handleFileChunk(ctx, content);

    UUID fileId = fileController.finishChunkedUpload(currentSessionId);

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, fileId: {}, chunks: {}, bytes: {}",
          fileId,
          currentSessionId,
          receivedChunks,
          receivedBytes);

    sendSuccessResponse(ctx, fileId, totalFileSize);
    requestInProgress = false;
  }

  private void handleFileChunk(ChannelHandlerContext ctx, HttpContent content) {
    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    receivedChunks++;
    receivedBytes += chunkSize;

    try {
      fileController.processFileChunk(
          new FileChunkDto(currentSessionId, receivedChunks, chunkBytes));
    } catch (FileUploadValidateException e) {
      // TODO
      return;
    }

    logger.debug(
        "Processed chunk {}/{} for session: {}. Size: {} bytes, total: {} bytes",
        receivedChunks,
        totalChunks,
        currentSessionId,
        chunkSize,
        totalFileSize);

    if (receivedChunks % SEND_UPLOAD_PROGRESS_INTERVAL == 0 || receivedChunks == totalChunks)
      sendProgressResponse(ctx, "upload_progress");
  }

  private void parseRequestMetadata(HttpRequest request) {
    // TODO: парсинг метаданных, аутентификация
    currentSessionId = "";
    currentUserId = "";
    currentFilePath = "";
    totalFileSize = Long.parseLong(request.headers().get("X-Total-File-Size", "0"));
    totalChunks = Integer.parseInt(request.headers().get("X-Total-Chunks", "1"));
  }

  private void sendProgressResponse(ChannelHandlerContext ctx, String status) {
    String json =
        String.format(
            "{\"status\":\"%s\",\"currentChunk\":%d,\"totalChunks\":%d,\"bytesReceived\":%d,\"sessionId\":\"%s\"}",
            status, receivedChunks, totalChunks, receivedBytes, currentSessionId);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, UUID fileId, long totalFileSize) {
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

    requestInProgress = false;
  }
}
