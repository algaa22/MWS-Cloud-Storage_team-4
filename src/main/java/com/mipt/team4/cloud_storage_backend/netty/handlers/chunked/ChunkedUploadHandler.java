package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
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
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
@RequiredArgsConstructor
public class ChunkedUploadHandler {
  private final FileController fileController;

  // TODO: ненужные поля (кроме isInProgress)
  private boolean isInProgress = false;
  private List<String> currentFileTags;
  private String currentSessionId;
  private String currentUserToken;
  private String currentFilePath;
  private long receivedBytes = 0;
  private int receivedChunks = 0;

  public void startChunkedUpload(HttpRequest request)
      throws TransferAlreadyStartedException,
      QueryParameterNotFoundException,
      HeaderNotFoundException,
      ValidationFailedException,
      StorageFileAlreadyExistsException,
      UserNotFoundException {
    if (isInProgress) {
      throw new TransferAlreadyStartedException();
    }

    parseUploadRequestMetadata(request);

    fileController.startChunkedUpload(
        new FileChunkedUploadRequest(
            currentSessionId, currentUserToken, currentFilePath, currentFileTags));

    isInProgress = true;
    receivedChunks = 0;
    receivedBytes = 0;

    if (log.isDebugEnabled()) {
      log.debug(
          "Started chunked upload. Session: {}, user: {}, file: {}",
          currentSessionId,
          currentUserToken,
          currentFilePath);
    }
  }

  public void handleFileChunk(HttpContent content)
      throws TransferNotStartedYetException,
      UploadSessionNotFoundException,
      CombineChunksToPartException,
      ValidationFailedException {
    if (!isInProgress) {
      throw new TransferNotStartedYetException();
    }
    // TODO: не слишком ли большой чанк?
    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    fileController.processFileChunk(
        new UploadChunkRequest(currentSessionId, currentFilePath, receivedChunks, chunkBytes));

    receivedChunks++;
    receivedBytes += chunkSize;

    if (log.isDebugEnabled()) {
      log.debug(
          "Processed chunk {} for session: {}. Size: {} bytes, total: {} bytes",
          receivedChunks,
          currentSessionId,
          chunkSize,
          receivedBytes);
    }
  }

  public void completeChunkedUpload(ChannelHandlerContext ctx, LastHttpContent content)
      throws TransferNotStartedYetException,
      UserNotFoundException,
      UploadSessionNotFoundException,
      CombineChunksToPartException,
      ValidationFailedException,
      StorageFileAlreadyExistsException,
      TooSmallFilePartException,
      MissingFilePartException {
    if (!isInProgress) {
      throw new TransferNotStartedYetException();
    }

    if (content.content().readableBytes() > 0) {
      handleFileChunk(content);
    }

    ChunkedUploadFileResultDto result;

    try {
      result = fileController.completeChunkedUpload(currentSessionId);
    } catch (Exception e) {
      cleanup();
      throw e;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Completed chunk upload. Session: {}, path: {}, chunks: {}, bytes: {}",
          currentSessionId,
          currentFilePath,
          receivedChunks,
          receivedBytes);
    }

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

    ResponseUtils.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }
}
