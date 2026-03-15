package com.mipt.team4.cloud_storage_backend.netty.handlers.rest.aggregated;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectoriesRequestHandler {
  private final DirectoryController directoryController;

  public void handleCreateDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String name = RequestUtils.getRequiredQueryParam(request, "name");
    Optional<String> parentId = RequestUtils.getQueryParam(request, "parentId");

    UUID createdId =
        directoryController.createDirectory(
            new CreateDirectoryRequest(userToken, parentId, name, UUID.randomUUID()));

    ResponseUtils.sendCreatedResponse(ctx, createdId, "Directory successfully created");
  }

  public void handleChangeDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
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

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Directory successfully updated");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {

    String directoryId = RequestUtils.getRequiredQueryParam(request, "id");

    directoryController.deleteDirectory(new DeleteDirectoryRequest(userToken, directoryId));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
