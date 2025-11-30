package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeDirectoryPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleDirectoryOperationDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.FileNotFoundException;

public record DirectoriesRequestHandler(DirectoryController directoryController) {
  public void handleCreateDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken) {
    try {
      directoryController.createDirectory(
          new SimpleDirectoryOperationDto(userToken, directoryPath));
    } catch (ValidationFailedException
        | UserNotFoundException
        | StorageFileAlreadyExistsException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Directory successfully created");
  }

  public void handleChangeDirectoryPathRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String oldDirectoryPath;
    String newDirectoryPath;

    try {
      oldDirectoryPath = RequestUtils.getRequiredQueryParam(request, "from");
      newDirectoryPath = RequestUtils.getRequiredQueryParam(request, "to");
    } catch (QueryParameterNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    try {
      directoryController.changeDirectoryPath(
          new ChangeDirectoryPathDto(userToken, oldDirectoryPath, newDirectoryPath));
    } catch (ValidationFailedException
        | UserNotFoundException
        | StorageFileAlreadyExistsException
        | StorageFileNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "Directory path successfully changed");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken) {
    try {
      // TODO: ошибки обрабатывать в основном хендлере
      directoryController.deleteDirectory(
          new SimpleDirectoryOperationDto(userToken, directoryPath));
    } catch (ValidationFailedException
        | UserNotFoundException
        | StorageFileNotFoundException
        | FileNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
