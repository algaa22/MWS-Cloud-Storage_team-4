package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
  private static final Logger logger = LoggerFactory.getLogger(AggregatedHttpHandler.class);
  private final FoldersRequestHandler foldersRequestHandler;
  private final FilesRequestHandler filesRequestHandler;
  private final UsersRequestHandler usersRequestHandler;

  private HttpMethod method;
  private String uri;

  public AggregatedHttpHandler(FileController fileController, UserController userController) {
    this.foldersRequestHandler = new FoldersRequestHandler(fileController);
    this.filesRequestHandler = new FilesRequestHandler(fileController);
    this.usersRequestHandler = new UsersRequestHandler(userController);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof FullHttpRequest request) {
      method = request.method();
      uri = request.uri();

      if (uri.startsWith("/api/files")) {
        handleFilesRequest(ctx, request);
      } else if (uri.startsWith("/api/folders")) {
        handleFoldersRequest(ctx, request);
      } else if (uri.startsWith("/api/users")) {
        handleUsersRequest(ctx, request);
      } else {
        ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    } else {
      ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private void handleFilesRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.equals("/api/files") && method.equals(HttpMethod.GET))
      filesRequestHandler.handleGetFilePathsListRequest(ctx, userToken);
    else {
      String filePath;

      try {
        filePath = RequestUtils.getRequiredQueryParam(request, "path");
      } catch (QueryParameterNotFoundException e) {
        ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
        return;
      }

      // TODO: экранировать /

      if (uri.startsWith("/api/files/info") && method.equals(HttpMethod.GET))
        filesRequestHandler.handleGetFileInfoRequest(ctx, filePath, userToken);
      else {
        if (method.equals(HttpMethod.DELETE))
          filesRequestHandler.handleDeleteFileRequest(ctx, filePath, userToken);
        else if (method.equals(HttpMethod.POST))
          filesRequestHandler.handleUploadFileRequest(ctx, request, filePath, userToken);
        else if (method.equals(HttpMethod.PUT))
          filesRequestHandler.handleChangeFileMetadataRequest(ctx, request, filePath, userToken);
        else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    }
  }

  private void handleFoldersRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.equals("/api/folders") && method.equals(HttpMethod.POST))
      foldersRequestHandler.handleCreateFolderRequest(ctx, userToken);
    else {
      String[] uriPaths = uri.split("/");
      String folderId = uriPaths[uriPaths.length - 1];

      if (uri.startsWith("/api/folders/move/") && method.equals(HttpMethod.POST))
        foldersRequestHandler.handleMoveFolderRequest(ctx, folderId, userToken);
      else if (uri.length() == 4) {
        if (method.equals(HttpMethod.GET))
          foldersRequestHandler.handleGetFolderContentRequest(ctx, folderId, userToken);
        else if (method.equals(HttpMethod.PUT))
          foldersRequestHandler.handleRenameFolderRequest(ctx, folderId, userToken);
        else if (method.equals(HttpMethod.DELETE))
          foldersRequestHandler.handleDeleteFolderRequest(ctx, folderId, userToken);
        else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      } else {
        ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    }
  }

  private void handleUsersRequest(ChannelHandlerContext ctx, HttpRequest request) {
    if (method.equals(HttpMethod.POST)) {
      if (uri.equals("/api/users/auth/login")) usersRequestHandler.handleLoginRequest(ctx, request);
      else if (uri.equals("/api/users/auth/register"))
        usersRequestHandler.handleRegisterRequest(ctx, request);
      else if (uri.equals("/api/users/auth/logout"))
        usersRequestHandler.handleLogoutRequest(ctx, request);
      else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    } else {
      ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private String extractUserTokenFromRequest(HttpRequest request) {
    return request.headers().get("X-Auth-Token", "");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unhandled exception in channel from {}", ctx.channel().remoteAddress(), cause);
    ResponseHelper.sendInternalServerErrorResponse(ctx);
  }
}
