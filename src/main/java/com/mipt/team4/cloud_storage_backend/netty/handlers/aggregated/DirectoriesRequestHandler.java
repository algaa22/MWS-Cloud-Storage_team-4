package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeDirectoryPathRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleDirectoryOperationRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DirectoriesRequestHandler {
  private final DirectoryController directoryController;

  public void handleCreateDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken) {
    directoryController.createDirectory(
        new SimpleDirectoryOperationRequest(userToken, directoryPath));

    ResponseUtils.sendSuccess(
        ctx, HttpResponseStatus.CREATED, "Directory successfully created");
  }

  public void handleChangeDirectoryPathRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String oldDirectoryPath;
    String newDirectoryPath;

    oldDirectoryPath = RequestUtils.getRequiredQueryParam(request, "from");
    newDirectoryPath = RequestUtils.getRequiredQueryParam(request, "to");

    directoryController.changeDirectoryPath(
        new ChangeDirectoryPathRequest(userToken, oldDirectoryPath, newDirectoryPath));

    ResponseUtils.sendSuccess(
        ctx, HttpResponseStatus.OK, "Directory path successfully changed");
  }

  public void handleDeleteDirectoryRequest(
      ChannelHandlerContext ctx, String directoryPath, String userToken) {

    directoryController.deleteDirectory(
        new SimpleDirectoryOperationRequest(userToken, directoryPath));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
