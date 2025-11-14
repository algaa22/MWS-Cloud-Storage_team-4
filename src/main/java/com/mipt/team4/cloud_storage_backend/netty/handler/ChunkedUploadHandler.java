package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.service.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.service.TranferSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.List;
import java.util.UUID;
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

    try {
      parseUploadRequestMetadata(request);
    } catch (QueryParameterNotFoundException | HeaderNotFoundException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    }

    try {
      fileController.startChunkedUpload(
          new FileChunkedUploadDto(
              currentSessionId,
              currentUserId,
              totalFileSize,
              totalChunks,
              currentFilePath,
              currentFileTags));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (StorageFileAlreadyExistsException e) {
      handleFileAlreadyExists(ctx, currentFilePath);
      return;
    } catch (StorageIllegalAccessException e) {
      // TODO
    }

    isInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

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

    UUID fileId;

    try {
      fileId = fileController.finishChunkedUpload(currentSessionId);
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (MissingFilePartException e) {
      handleMissingFilePart(ctx, e);
      return;
    } catch (TranferSessionNotFoundException e) {
      handleSessionNotFound(ctx, e);
      return;
    }

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, filePath: {}, chunks: {}, bytes: {}",
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

    try {
      fileController.processFileChunk(
          new FileChunkDto(currentSessionId, currentFilePath, receivedChunks, chunkBytes));
    } catch (ValidationFailedException e) {
      ResponseHelper.sendValidationErrorResponse(ctx, e);
      return;
    } catch (TranferSessionNotFoundException e) {
      handleSessionNotFound(ctx, e);
      return;
    }

    receivedChunks++;
    receivedBytes += chunkSize;

    logger.debug(
        "Processed chunk {}/{} for session: {}. Size: {} bytes, total: {} bytes",
        receivedChunks,
        totalChunks,
        currentSessionId,
        chunkSize,
        totalFileSize);

    if (receivedChunks % StorageConfig.INSTANCE.getSendUploadProgressInterval() == 0
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

  private void parseUploadRequestMetadata(HttpRequest request)
      throws QueryParameterNotFoundException, HeaderNotFoundException {
    // TODO: парсинг метаданных пользователя, аутентификация
    currentSessionId = "";
    currentUserId = "";
    currentFilePath = RequestUtils.getRequiredQueryParam(request, "File path");
    currentFileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
    totalFileSize = Long.parseLong(RequestUtils.getRequiredHeader(request, "X-File-Size"));
    totalChunks = Integer.parseInt(RequestUtils.getRequiredHeader(request, "X-Total-Chunks"));
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, UUID fileId, long totalFileSize) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    json.put("status", "complete");
    json.put("filePath", fileId.toString());
    json.put("fileSize", totalFileSize);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void sendProgressResponse(ChannelHandlerContext ctx, String status) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    json.put("status", status);
    json.put("currentChunk", receivedChunks);
    json.put("totalChunks", totalChunks);
    json.put("bytesReceived", receivedBytes);
    json.put("sessionId", currentSessionId);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }

  private void handleSessionNotFound(ChannelHandlerContext ctx, TranferSessionNotFoundException e) {
    ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
    cleanup();
  }

  private void handleMissingFilePart(ChannelHandlerContext ctx, MissingFilePartException e) {
    ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    cleanup();
  }

  private void handleValidationError(ChannelHandlerContext ctx, ValidationFailedException e) {
    ResponseHelper.sendValidationErrorResponse(ctx, e);
    cleanup();
  }

  private void handleFileAlreadyExists(ChannelHandlerContext ctx, String filePath) {
    ResponseHelper.sendErrorResponse(
        ctx, HttpResponseStatus.BAD_REQUEST, "File " + filePath + " already exists");
    cleanup();
  }
}
