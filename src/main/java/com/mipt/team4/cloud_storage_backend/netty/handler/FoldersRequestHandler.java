package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeFolderPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleFolderOperationDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public record FoldersRequestHandler(FileController fileController) {
  public void handleCreateFolderRequest(
      ChannelHandlerContext ctx, String folderPath, String userToken) {
    try {
      fileController.createFolder(new SimpleFolderOperationDto(userToken, folderPath));
    } catch (ValidationFailedException | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Folder successfully created");
  }

  public void handleChangeFolderPathRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String oldFolderPath;
    String newFolderPath;

    try {
      oldFolderPath = RequestUtils.getRequiredQueryParam(request, "from");
      newFolderPath = RequestUtils.getRequiredQueryParam(request, "to");
    } catch (QueryParameterNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    try {
      fileController.changeFolderPath(
          new ChangeFolderPathDto(userToken, oldFolderPath, newFolderPath));
    } catch (ValidationFailedException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Folder path successfully changed");
  }

  public void handleDeleteFolderRequest(
      ChannelHandlerContext ctx, String folderPath, String userToken) {
    try {
      fileController.deleteFolder(new SimpleFolderOperationDto(userToken, folderPath));
    } catch (ValidationFailedException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Folder successfully deleted");
  }
}
