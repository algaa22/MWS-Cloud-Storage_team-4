package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeDirectoryPathRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleDirectoryOperationRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DirectoriesRequestHandler {
  private final DirectoryController directoryController;

  public void handleCreateDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken)
      throws UserNotFoundException, StorageFileAlreadyExistsException, ValidationFailedException {
    directoryController.createDirectory(
        new SimpleDirectoryOperationRequest(userToken, directoryPath));

    ResponseUtils.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Directory successfully created");
  }

  public void handleChangeDirectoryPathRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
      StorageFileNotFoundException,
          ValidationFailedException {
    String oldDirectoryPath;
    String newDirectoryPath;

    oldDirectoryPath = RequestUtils.getRequiredQueryParam(request, "from");
    newDirectoryPath = RequestUtils.getRequiredQueryParam(request, "to");

    directoryController.changeDirectoryPath(
        new ChangeDirectoryPathRequest(userToken, oldDirectoryPath, newDirectoryPath));

    ResponseUtils.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "Directory path successfully changed");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken)
      throws UserNotFoundException,
      StorageFileNotFoundException,
          ValidationFailedException,
          FileNotFoundException {

    directoryController.deleteDirectory(
        new SimpleDirectoryOperationRequest(userToken, directoryPath));

    ResponseUtils.sendSuccessResponse(ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
