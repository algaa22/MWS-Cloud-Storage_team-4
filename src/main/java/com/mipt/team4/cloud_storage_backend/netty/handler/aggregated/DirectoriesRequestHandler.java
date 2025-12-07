package com.mipt.team4.cloud_storage_backend.netty.handler.aggregated;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
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
      ChannelHandlerContext ctx, String directoryPath, String userToken)
      throws UserNotFoundException, StorageFileAlreadyExistsException, ValidationFailedException {
    directoryController.createDirectory(new SimpleDirectoryOperationDto(userToken, directoryPath));

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Directory successfully created");
  }

  public void handleChangeDirectoryPathRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageEntityNotFoundException,
          ValidationFailedException {
    String oldDirectoryPath;
    String newDirectoryPath;

    oldDirectoryPath = RequestUtils.getRequiredQueryParam(request, "from");
    newDirectoryPath = RequestUtils.getRequiredQueryParam(request, "to");

    directoryController.changeDirectoryPath(
        new ChangeDirectoryPathDto(userToken, oldDirectoryPath, newDirectoryPath));

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "Directory path successfully changed");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken)
      throws UserNotFoundException,
          StorageEntityNotFoundException,
          ValidationFailedException,
          FileNotFoundException {

    directoryController.deleteDirectory(new SimpleDirectoryOperationDto(userToken, directoryPath));

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
