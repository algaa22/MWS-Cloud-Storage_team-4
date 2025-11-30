package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
  private static final Logger logger = LoggerFactory.getLogger(AggregatedHttpHandler.class);
  private final DirectoriesRequestHandler directorysRequestHandler;
  private final FilesRequestHandler filesRequestHandler;
  private final UsersRequestHandler usersRequestHandler;

  private HttpMethod method;
  private String uri;

  public AggregatedHttpHandler(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    this.directorysRequestHandler = new DirectoriesRequestHandler(directoryController);
    this.filesRequestHandler = new FilesRequestHandler(fileController);
    this.usersRequestHandler = new UsersRequestHandler(userController);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof FullHttpRequest request) {
      method = request.method();
      uri = request.uri();

      try {
        if (uri.startsWith("/api/files")) {
          handleFilesRequest(ctx, request);
        } else if (uri.startsWith("/api/directories")) {
          handleDirectoriesRequest(ctx, request);
        } else if (uri.startsWith("/api/users")) {
          handleUsersRequest(ctx, request);
        } else {
          ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
        }
      } catch (QueryParameterNotFoundException
          | InvalidSessionException
          | ValidationFailedException
          | HeaderNotFoundException
          | UserNotFoundException
          | UserAlreadyExistsException
          | InvalidEmailOrPassword
          | WrongPasswordException
          | StorageFileNotFoundException
          | FileNotFoundException
          | StorageFileAlreadyExistsException
          | StorageIllegalAccessException e) {
        ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      }
    } else {
      ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private void handleFilesRequest(ChannelHandlerContext ctx, FullHttpRequest request)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          FileNotFoundException,
          StorageFileAlreadyExistsException,
          HeaderNotFoundException {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.startsWith("/api/files/list") && method.equals(HttpMethod.GET))
      filesRequestHandler.handleGetFilePathsListRequest(ctx, request, userToken);
    else {
      String filePath = RequestUtils.getRequiredQueryParam(request, "path");

      if (uri.startsWith("/api/files/info") && method.equals(HttpMethod.GET))
        filesRequestHandler.handleGetFileInfoRequest(ctx, filePath, userToken);
      else {
        if (method.equals(HttpMethod.DELETE))
          filesRequestHandler.handleDeleteFileRequest(ctx, filePath, userToken);
        else if (method.equals(HttpMethod.GET))
          filesRequestHandler.handleDownloadFileRequest(ctx, filePath, userToken);
        else if (method.equals(HttpMethod.POST))
          filesRequestHandler.handleUploadFileRequest(ctx, request, filePath, userToken);
        else if (method.equals(HttpMethod.PUT))
          filesRequestHandler.handleChangeFileMetadataRequest(ctx, request, filePath, userToken);
        else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    }
  }

  private void handleDirectoriesRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          FileNotFoundException,
          StorageFileAlreadyExistsException {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.startsWith("/api/directories") && method.equals(HttpMethod.POST))
      directorysRequestHandler.handleChangeDirectoryPathRequest(ctx, request, userToken);
    else {
      String directoryPath = RequestUtils.getRequiredQueryParam(request, "path");

      if (uri.startsWith("/api/directories") && method.equals(HttpMethod.PUT))
        directorysRequestHandler.handleCreateDirectoryRequest(ctx, directoryPath, userToken);
      else if (method.equals(HttpMethod.DELETE))
        directorysRequestHandler.handleDeleteDirectoryRequest(ctx, directoryPath, userToken);
      else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private void handleUsersRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws InvalidSessionException,
          ValidationFailedException,
          HeaderNotFoundException,
          UserNotFoundException,
          UserAlreadyExistsException,
          InvalidEmailOrPassword,
          WrongPasswordException {
    // TODO: switch-case?
    if (method.equals(HttpMethod.POST)) {
      if (uri.equals("/api/users/auth/login")) usersRequestHandler.handleLoginRequest(ctx, request);
      else if (uri.equals("/api/users/auth/register"))
        usersRequestHandler.handleRegisterRequest(ctx, request);
      else if (uri.equals("/api/users/auth/logout"))
        usersRequestHandler.handleLogoutRequest(ctx, request);
      else if (uri.equals("/api/users/auth/update"))
        usersRequestHandler.handleUpdateUserRequest(ctx, request);
      else if (uri.equals("/api/users/auth/refresh"))
        usersRequestHandler.handleRefreshTokenRequest(ctx, request);
      else ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    } else if (method.equals(HttpMethod.GET)) {
      if (uri.equals("/api/users/info")) usersRequestHandler.handleGetUserRequest(ctx, request);
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
