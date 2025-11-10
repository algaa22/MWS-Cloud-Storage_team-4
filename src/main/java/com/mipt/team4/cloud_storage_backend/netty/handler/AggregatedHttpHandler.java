package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatedHttpHandler extends SimpleChannelInboundHandler<HttpRequest> {
  private static final Logger logger = LoggerFactory.getLogger(AggregatedHttpHandler.class);
  private final FilesRequestHandler filesRequestHandler;

  private HttpMethod method;
  private String uri;

  public AggregatedHttpHandler(FileController fileController, UserController userController, FilesRequestHandler filesRequestHandler) {
    this.filesRequestHandler = new FilesRequestHandler(fileController);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request)
      throws Exception {
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
  }

  private void handleFilesRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String userId = extractUserIdFromRequest(request);

    try {
      validateUserId(userId);
    } catch (ValidationFailedException e) {
      ResponseHelper.sendValidationErrorResponse(ctx, e);
      return;
    }

    if (uri.equals("/api/files") && method.equals(HttpMethod.GET))
      filesRequestHandler.handleGetFilePathsListRequest(ctx, userId);
    else {
      String[] uriPaths = uri.split("/");
      String fileId = uriPaths[uriPaths.length - 1];

      if (uri.startsWith("/api/files/info/") && method.equals(HttpMethod.GET))
        filesRequestHandler.handleGetFileInfoRequest(ctx, fileId, userId);
      else if (uriPaths.length == 4) {
        if (method.equals(HttpMethod.DELETE))
          filesRequestHandler.handleDeleteFileRequest(ctx, fileId, userId);
        else if (method.equals(HttpMethod.PUT))
          filesRequestHandler.handleChangeFileMetadataRequest(ctx, fileId, userId);
        else if (method.equals(HttpMethod.GET))
          filesRequestHandler.handleGetFileRequest(ctx, fileId, userId);
        else
          ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      } else {
        ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    }
  }

  private void handleFoldersRequest(ChannelHandlerContext ctx, HttpRequest request) {

  }

  private void handleUsersRequest(ChannelHandlerContext ctx, HttpRequest request) {

  }

  private void validateUserId(String userId) throws ValidationFailedException {
    ValidationResult validationResult = Validators.notEmpty("User ID", userId);

    if (!validationResult.isValid())
      throw new ValidationFailedException(validationResult.getErrors());
  }

  private String extractUserIdFromRequest(HttpRequest request) {
    // TODO: аутентификация
    return request.headers().get("X-User-Id", "");
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unhandled exception in channel from {}", ctx.channel().remoteAddress(), cause);
    ResponseHelper.sendInternalServerErrorResponse(ctx);
  }
}
