package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.List;

public class AggregatedUploadHandler {
  private final FileController fileController;

  private boolean isInProgress = false;
  private String currentUserToken;
  private String currentFilePath;
  private List<String> currentFileTags;

  public AggregatedUploadHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void handleRequest(ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    if (isInProgress) {
      ResponseHelper.sendErrorResponse(
          ctx, HttpResponseStatus.BAD_REQUEST, "Upload already started");
      return;
    }

    this.currentUserToken = userToken;

    try {
      parseUploadMetadata(request);
    } catch (QueryParameterNotFoundException | HeaderNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    isInProgress = true;
  }

  public void handleContent(ChannelHandlerContext ctx, LastHttpContent content) {
    if (!isInProgress) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Upload not started");
      return;
    }

    // TODO: проверка на размер

    byte[] data = content.content().array();

    try {
      fileController.uploadFile(
          new FileUploadDto(currentFilePath, currentUserToken, currentFileTags, data));
    } catch (UserNotFoundException | StorageFileAlreadyExistsException | ValidationFailedException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    sendSuccessResponse(ctx, data.length);
  }

  private void parseUploadMetadata(HttpRequest request)
      throws QueryParameterNotFoundException, HeaderNotFoundException {
    currentFilePath = RequestUtils.getRequiredQueryParam(request, "File path");
    currentFileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
  }

  private void sendSuccessResponse(ChannelHandlerContext ctx, long fileSize) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode();

    json.put("status", "complete");
    json.put("filePath", currentFilePath);
    json.put("fileSize", fileSize);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, json);
  }
}
