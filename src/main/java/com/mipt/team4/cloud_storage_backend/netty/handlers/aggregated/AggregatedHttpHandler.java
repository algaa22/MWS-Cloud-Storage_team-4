package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileLockedException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class AggregatedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {

  private final DirectoriesRequestHandler directoriesRequestHandler;
  private final FilesRequestHandler filesRequestHandler;
  private final UsersRequestHandler usersRequestHandler;

  private HttpMethod method;
  private String uri;

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
          ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
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
               | StorageFileAlreadyExistsException
               | StorageIllegalAccessException
               | IOException e) {
        ResponseUtils.sendBadRequestExceptionResponse(ctx, e);
      } catch (StorageFileLockedException e) {
        ResponseUtils.sendErrorResponse(ctx, HttpResponseStatus.CONFLICT, e.getMessage());
      }
    } else {
      ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private void handleFilesRequest(ChannelHandlerContext ctx, FullHttpRequest request)
      throws QueryParameterNotFoundException,
          UserNotFoundException,
      StorageFileNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          IOException,
          StorageFileAlreadyExistsException,
          HeaderNotFoundException {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.startsWith("/api/files/list") && method.equals(HttpMethod.GET)) {
      filesRequestHandler.handleGetFilePathsListRequest(ctx, request, userToken);
    } else {
      String filePath = RequestUtils.getRequiredQueryParam(request, "path");

      if (uri.startsWith("/api/files/info") && method.equals(HttpMethod.GET)) {
        filesRequestHandler.handleGetFileInfoRequest(ctx, filePath, userToken);
      } else {
        switch (method.name()) {
          case "DELETE" -> filesRequestHandler.handleDeleteFileRequest(ctx, filePath, userToken);
          case "POST" ->
              filesRequestHandler.handleUploadFileRequest(ctx, request, filePath, userToken);
          case "PUT" ->
              filesRequestHandler.handleChangeFileMetadataRequest(
                  ctx, request, filePath, userToken);
          default -> ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
        }
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

    if (uri.startsWith("/api/directories") && method.equals(HttpMethod.POST)) {
      directoriesRequestHandler.handleChangeDirectoryPathRequest(ctx, request, userToken);
    } else {
      String directoryPath = RequestUtils.getRequiredQueryParam(request, "path");

      if (uri.startsWith("/api/directories") && method.equals(HttpMethod.PUT)) {
        directoriesRequestHandler.handleCreateDirectoryRequest(ctx, directoryPath, userToken);
      } else if (method.equals(HttpMethod.DELETE)) {
        directoriesRequestHandler.handleDeleteDirectoryRequest(ctx, directoryPath, userToken);
      } else {
        ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
      }
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
    if (method.equals(HttpMethod.POST)) {
      switch (uri) {
        case "/api/users/auth/login" -> usersRequestHandler.handleLoginRequest(ctx, request);
        case "/api/users/auth/register" -> usersRequestHandler.handleRegisterRequest(ctx, request);
        case "/api/users/auth/logout" -> usersRequestHandler.handleLogoutRequest(ctx, request);
        case "/api/users/update" -> usersRequestHandler.handleUpdateUserRequest(ctx, request);
        case "/api/users/auth/refresh" ->
            usersRequestHandler.handleRefreshTokenRequest(ctx, request);
        default -> ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    } else if (method.equals(HttpMethod.GET)) {
      if (uri.equals("/api/users/info")) {
        usersRequestHandler.handleGetUserRequest(ctx, request);
      }
    } else {
      ResponseUtils.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private String extractUserTokenFromRequest(HttpRequest request) {
    return request.headers().get("X-Auth-Token", "");
  }
}
