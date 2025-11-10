package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.netty.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.http.validation.FileUploadValidationException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedUploadHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChunkedUploadHandler.class);
  private final FileController fileController;

  private boolean isInProgress = false;
  private List<String> currentFileTags;
  private String currentSessionId;
  private String currentUserId;
  private String currentFilePath;
  private long totalFileSize;
  private long receivedBytes = 0;
  private int receivedChunks = 0;
  private int totalChunks;

  public ChunkedUploadHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void startChunkedUpload(ChannelHandlerContext ctx, HttpRequest request)
      throws TransferAlreadyStartedException {
    if (isInProgress) throw new TransferAlreadyStartedException();

    parseUploadRequestMetadata(request);

    isInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

    try {
      fileController.startChunkedUpload(
          new FileChunkedUploadSession(
              currentSessionId,
              currentUserId,
              totalFileSize,
              totalChunks,
              currentFilePath,
              currentFileTags,
              new ArrayList<>()));
    } catch (FileUploadValidationException e) {

      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked upload. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          currentSessionId,
          currentUserId,
          currentFilePath,
          totalFileSize,
          totalChunks);
    }

    sendProgressResponse(ctx, "upload_started");
  }

  public void finishChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content)
      throws TransferNotStartedYetException {
    if (!isInProgress) throw new TransferNotStartedYetException();

    if (content.content().readableBytes() > 0) handleFileChunk(ctx, content);

    String fileId = fileController.finishChunkedUpload(currentSessionId);

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, fileId: {}, chunks: {}, bytes: {}",
          fileId,
          currentSessionId,
          receivedChunks,
          receivedBytes);

    sendSuccessResponse(ctx, fileId, totalFileSize);
    cleanup();
  }

  public void handleFileChunk(ChannelHandlerContext ctx, HttpContent content)
      throws TransferNotStartedYetException {
    if (!isInProgress) throw new TransferNotStartedYetException();

    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    receivedChunks++;
    receivedBytes += chunkSize;

    try {
      fileController.processFileChunk(
          new FileChunk(currentSessionId, currentFilePath, receivedChunks, chunkBytes));
    } catch (FileUploadValidationException e) {
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

    if (receivedChunks % StorageConfig.getInstance().getSendUploadProgressInterval() == 0
        || receivedChunks == totalChunks) sendProgressResponse(ctx, "upload_progress");
  }

  public void cleanup() {
    isInProgress = false;
    currentSessionId = null;
    currentUserId = null;
    currentFilePath = null;
    currentFileTags = null;
    totalFileSize = 0;
    receivedBytes = 0;
    receivedChunks = 0;
    totalChunks = 0;
  }

  private void parseUploadRequestMetadata(HttpRequest request) {
    // TODO: парсинг метаданных пользователя, аутентификация
    currentSessionId = "";
    currentUserId = "";
    currentFilePath = request.headers().get("X-File-Path");
    currentFileTags = parseFileTags(request.headers().get("X-File-Tags"));
    totalFileSize = Long.parseLong(request.headers().get("X-File-Size"));
    totalChunks = Integer.parseInt(request.headers().get("X-Total-Chunks"));
  }

  private List<String> parseFileTags(String tags) {
    return Arrays.stream(tags.split(","))
            .filter(tag -> !tag.trim().isEmpty())
            .toList();
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, String fileId, long totalFileSize) {
    String json =
        String.format(
            "{\"status\":\"complete\",\"fileId\":%s,\"fileSize\":%d}", fileId, totalFileSize);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void sendProgressResponse(ChannelHandlerContext ctx, String status) {
    String json =
        String.format(
            "{\"status\":\"%s\",\"currentChunk\":%d,\"totalChunks\":%d,\"bytesReceived\":%d,\"sessionId\":\"%s\"}",
            status, receivedChunks, totalChunks, receivedBytes, currentSessionId);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }
}
