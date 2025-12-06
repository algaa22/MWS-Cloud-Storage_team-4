package com.mipt.team4.cloud_storage_backend.netty.handler.chunked;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResultDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
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

  // TODO: ненужные поля (кроме isInProgress)
  private boolean isInProgress = false;
  private List<String> currentFileTags;
  private String currentSessionId;
  private String currentUserToken;
  private String currentFilePath;
  private long receivedBytes = 0;
  private int receivedChunks = 0;

  public ChunkedUploadHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void startChunkedUpload(HttpRequest request)
      throws TransferAlreadyStartedException,
          QueryParameterNotFoundException,
          HeaderNotFoundException,
          ValidationFailedException,
          StorageFileAlreadyExistsException,
          UserNotFoundException,
          StorageIllegalAccessException {
    if (isInProgress) throw new TransferAlreadyStartedException();

    parseUploadRequestMetadata(request);

    fileController.startChunkedUpload(
        new FileChunkedUploadDto(
            currentSessionId, currentUserToken, currentFilePath, currentFileTags));

    isInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Started chunked upload. Session: {}, user: {}, file: {}",
          currentSessionId,
          currentUserToken,
          currentFilePath);
    }
  }

  public void handleFileChunk(ChannelHandlerContext ctx, HttpContent content)
      throws TransferNotStartedYetException,
          UserNotFoundException,
          UploadSessionNotFoundException,
          CombineChunksToPartException,
          ValidationFailedException {
    if (!isInProgress) throw new TransferNotStartedYetException();
    // TODO: не слишком ли большой чанк?
    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    fileController.processFileChunk(
        new UploadChunkDto(currentSessionId, currentFilePath, receivedChunks, chunkBytes));

    receivedChunks++;
    receivedBytes += chunkSize;

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Processed chunk {} for session: {}. Size: {} bytes, total: {} bytes",
          receivedChunks,
          currentSessionId,
          chunkSize,
          receivedBytes);
    }
  }

  // TODO: а если коннект резко оборвётся?
  public void completeChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content)
      throws TransferNotStartedYetException,
          UserNotFoundException,
          UploadSessionNotFoundException,
          CombineChunksToPartException,
          ValidationFailedException,
          StorageFileAlreadyExistsException,
          TooSmallFilePartException,
          MissingFilePartException {
    if (!isInProgress) throw new TransferNotStartedYetException();

    if (content.content().readableBytes() > 0) handleFileChunk(ctx, content);

    ChunkedUploadFileResultDto result;

    try {
      result = fileController.completeChunkedUpload(currentSessionId);
    } catch (Exception e) {
      cleanup();
      throw e;
    }

    if (logger.isDebugEnabled())
      logger.debug(
          "Completed chunk upload. Session: {}, path: {}, chunks: {}, bytes: {}",
          currentSessionId,
          currentFilePath,
          receivedChunks,
          receivedBytes);

    sendSuccessResponse(ctx, result);
    cleanup();
  }

  public void cleanup() {
    isInProgress = false;
    currentSessionId = null;
    currentUserToken = null;
    currentFilePath = null;
    currentFileTags = null;
    receivedBytes = 0;
    receivedChunks = 0;
  }

  private void parseUploadRequestMetadata(HttpRequest request)
      throws QueryParameterNotFoundException, HeaderNotFoundException {
    currentSessionId = UUID.randomUUID().toString();
    currentUserToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    currentFilePath = RequestUtils.getRequiredQueryParam(request, "path");
    currentFileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, ChunkedUploadFileResultDto result) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    json.put("status", "complete");
    json.put("path", result.filePath());
    json.put("fileSize", result.fileSize());
    json.put("totalParts", result.totalParts());

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }
}
