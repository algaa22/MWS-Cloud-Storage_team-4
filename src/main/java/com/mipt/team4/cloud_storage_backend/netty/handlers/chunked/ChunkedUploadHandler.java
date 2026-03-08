package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadNotStoppedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResult;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadRequest;
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
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private String currentName;
  private Optional<String> currentParentId;

  public void start(HttpRequest request) {
    if (isInProgress) {
      throw new TransferAlreadyStartedException();
    }

    parseStartUploadRequestMetadata(request);

    fileController.startChunkedUpload(
        new ChunkedUploadRequest(
            currentSessionId, currentUserToken, currentParentId, currentName, currentFileTags));

    isInProgress = true;

    if (log.isDebugEnabled()) {
      log.debug(
          "Started chunked upload. Session: {}, name: {}, parentId: {}",
          currentSessionId,
          currentName,
          currentParentId);
    }
  }

  public void handleChunk(ChannelHandlerContext ctx, HttpContent content) {
    if (!isInProgress) {
      throw new TransferNotStartedYetException();
    }
    // TODO: не слишком ли большой чанк?
    ByteBuf chunkData = content.content();
    int chunkSize = chunkData.readableBytes();

    byte[] chunkBytes = new byte[chunkSize];
    chunkData.getBytes(chunkData.readerIndex(), chunkBytes);

    try {
      fileController.processFileChunk(
          new UploadChunkRequest(currentSessionId, currentName, currentParentId, chunkBytes));
    } catch (ProcessUploadRetriableException exception) {
      sendNeedProcessRetryResponse(ctx, exception);
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("Processed chunk for session: {}. Size: {} bytes", currentSessionId, chunkSize);
    }
  }

  public void complete(ChannelHandlerContext ctx, LastHttpContent content) {
    if (!isInProgress) {
      throw new TransferNotStartedYetException();
    }

    if (content.content().readableBytes() > 0) {
      handleChunk(ctx, content);
    }

    ChunkedUploadFileResult result;

    try {
      result = fileController.completeChunkedUpload(currentSessionId);
    } catch (CompleteUploadRetriableException exception) {
      sendNeedCompleteRetryResponse(ctx, exception);
      return;
    }

    sendSuccessResponse(ctx, result);
    cleanup();

    if (log.isDebugEnabled()) {
      log.debug(
          "Completed chunk upload. Session: {}, name: {}, parent: {}",
          currentSessionId,
          currentName,
          currentParentId);
    }
  }

  public void resume(HttpRequest request) {
    if (isInProgress) {
      throw new UploadNotStoppedException();
    }

    parseRequestMetadata(request);
    isInProgress = true;

    fileController.resumeChunkedUpload(
        new ChunkedUploadRequest(
            currentSessionId, currentUserToken, currentParentId, currentName, currentFileTags));

    if (log.isDebugEnabled()) {
      log.debug(
          "Resumed chunked upload. Session: {}, user: {}, name: {}, parentId: {}",
          currentSessionId,
          currentUserToken,
          currentName,
          currentParentId);
    }
  }

  public void cleanup() {
    isInProgress = false;
    currentSessionId = null;
    currentUserToken = null;
    currentName = null;
    currentFileTags = null;
    currentParentId = Optional.empty();
  }

  private void parseStartUploadRequestMetadata(HttpRequest request)
      throws QueryParameterNotFoundException, HeaderNotFoundException {
    parseRequestMetadata(request);

    currentFileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
  }

  private void parseRequestMetadata(HttpRequest request) {
    currentSessionId = UUID.randomUUID().toString();
    currentUserToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    currentName = RequestUtils.getRequiredQueryParam(request, "name");
    currentParentId = RequestUtils.getQueryParam(request, "parentId");
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, ChunkedUploadFileResult result) {
    ObjectNode json = ResponseUtils.createJsonNode(true, "File uploaded successfully");

    json.put("id", result.fileId().toString());
    json.put("fileSize", result.fileSize());
    json.put("totalParts", result.totalParts());

    ResponseUtils.sendJson(ctx, HttpResponseStatus.CREATED, json);
  }

  private void sendNeedProcessRetryResponse(
      ChannelHandlerContext ctx, ProcessUploadRetriableException exception) {
    ObjectNode json = ResponseUtils.createJsonNode(false, exception.getMessage());

    json.put("action", "RESUME_CONTINUE");
    json.put("currentFileSize", exception.getCurrentFileSize());
    json.put("partNum", exception.getPartNum());

    ResponseUtils.sendJson(ctx, exception.getStatus(), json);
  }

  private void sendNeedCompleteRetryResponse(
      ChannelHandlerContext ctx, CompleteUploadRetriableException exception) {
    ObjectNode json = ResponseUtils.createJsonNode(false, exception.getMessage());

    json.put("action", "RESUME_FINALIZE");

    ResponseUtils.sendJson(ctx, exception.getStatus(), json);
  }
}
