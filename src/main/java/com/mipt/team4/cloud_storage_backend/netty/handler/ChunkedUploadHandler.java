package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
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
  private String currentUserToken;
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
    } catch (QueryParameterNotFoundException | HeaderNotFoundException | ParseException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    try {
      fileController.startChunkedUpload(
          new FileChunkedUploadDto(
              currentSessionId,
              currentUserToken,
              totalFileSize,
              totalChunks,
              currentFilePath,
              currentFileTags));
    } catch (ValidationFailedException
        | StorageFileAlreadyExistsException
        | StorageIllegalAccessException
        | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      cleanup();
      return;
    }

    isInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked upload. Session: {}, user: {}, file: {}, size: {}, chunks: {}",
          currentSessionId,
          currentUserToken,
          currentFilePath,
          totalFileSize,
          totalChunks);
    }

    sendProgressResponse(ctx, "upload_started");
  }

  public void completeChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content)
      throws TransferNotStartedYetException {
    if (!isInProgress) throw new TransferNotStartedYetException();

    if (content.content().readableBytes() > 0) handleFileChunk(ctx, content);

    try {
      fileController.completeChunkedUpload(currentSessionId);
    } catch (UserNotFoundException
        | StorageFileAlreadyExistsException
        | ValidationFailedException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      cleanup();
      return;
    } catch (MissingFilePartException e) {
      ResponseHelper.sendExceptionResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
      cleanup();
      return;
    }

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, newPath: {}, chunks: {}, bytes: {}",
          currentSessionId,
          currentFilePath,
          receivedChunks,
          receivedBytes);

    sendSuccessResponse(ctx, currentFilePath, totalFileSize);
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
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
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
    currentUserToken = null;
    currentFilePath = null;
    currentFileTags = null;
    totalFileSize = 0;
    receivedBytes = 0;
    receivedChunks = 0;
    totalChunks = 0;
  }

  private void parseUploadRequestMetadata(HttpRequest request)
      throws QueryParameterNotFoundException, HeaderNotFoundException, ParseException {
    currentSessionId = UUID.randomUUID().toString();
    currentUserToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    currentFilePath = RequestUtils.getRequiredQueryParam(request, "newPath");
    currentFileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
    totalFileSize =
        SafeParser.parseLong("File size", RequestUtils.getRequiredHeader(request, "X-File-Size"));
    totalChunks =
        SafeParser.parseInt(
            "Total chunks", RequestUtils.getRequiredHeader(request, "X-Total-Chunks"));
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, String filePath, long totalFileSize) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    json.put("status", "complete");
    json.put("newPath", filePath);
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
}
