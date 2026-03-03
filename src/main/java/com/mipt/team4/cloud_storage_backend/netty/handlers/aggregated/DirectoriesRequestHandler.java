package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.MoveDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DirectoriesRequestHandler {
  private final DirectoryController directoryController;

  public void handleCreateDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException,
          StorageFileAlreadyExistsException,
          ValidationFailedException,
          QueryParameterNotFoundException {
    String name = RequestUtils.getRequiredQueryParam(request, "name");
    Optional<String> parentId = RequestUtils.getQueryParam(request, "parentId");

    UUID createdId =
        directoryController.createDirectory(
            new CreateDirectoryRequest(userToken, parentId, name, UUID.randomUUID()));

    ResponseUtils.sendCreatedResponse(ctx, createdId, "Directory successfully created");
  }

  public void handleChangeDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageFileNotFoundException,
          ValidationFailedException {
    String directoryId = RequestUtils.getRequiredQueryParam(request, "id");
    Optional<String> newName = RequestUtils.getQueryParam(request, "newName");
    Optional<String> newParentId = RequestUtils.getQueryParam(request, "newParentId");

    // TODO: убрать != null, объединить эти два метода и вынести в контроллер?
    if (newName.isPresent()) {
      directoryController.renameDirectory(
          new RenameDirectoryRequest(userToken, directoryId, newName.get()));
    }

    if (newParentId.isPresent()) {
      directoryController.moveDirectory(
          new MoveDirectoryRequest(userToken, directoryId, newParentId.get()));
    }

    ResponseUtils.sendSuccessResponse(ctx, HttpResponseStatus.OK, "Directory successfully updated");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException {

    String directoryId = RequestUtils.getRequiredQueryParam(request, "id");

    directoryController.deleteDirectory(new DeleteDirectoryRequest(userToken, directoryId));

    ResponseUtils.sendSuccessResponse(ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
