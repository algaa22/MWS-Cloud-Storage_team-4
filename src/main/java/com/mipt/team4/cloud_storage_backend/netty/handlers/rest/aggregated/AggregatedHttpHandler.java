package com.mipt.team4.cloud_storage_backend.netty.handlers.rest.aggregated;

import com.mipt.team4.cloud_storage_backend.exception.netty.NotHttpRequestException;
import com.mipt.team4.cloud_storage_backend.netty.handlers.rest.DirectoriesRequestHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.rest.FilesRequestHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.rest.UsersRequestHandler;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class AggregatedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {

  private final DirectoriesRequestHandler directoriesRequestHandler;
  private final FilesRequestHandler filesRequestHandler;
  private final UsersRequestHandler usersRequestHandler;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (!(msg instanceof FullHttpRequest request)) {
      throw new NotHttpRequestException();
    }

    ReferenceCountUtil.retain(msg);
    startVirtualProcessor(ctx, request);
  }

  private void startVirtualProcessor(ChannelHandlerContext ctx, FullHttpRequest request) {
    Thread.startVirtualThread( // TODO: interrupted exception
        () -> {
          try {
            HttpMethod method = request.method();
            String uri = request.uri();

            if (uri.startsWith("/api/files")) {
              handleFilesRequest(ctx, request, uri, method);
            } else if (uri.startsWith("/api/directories")) {
              handleDirectoriesRequest(ctx, request, uri, method);
            } else if (uri.startsWith("/api/users")) {
              handleUsersRequest(ctx, request, uri, method);
            } else {
              ResponseUtils.sendMethodNotSupported(ctx, uri, method);
            }
          } catch (Exception e) {
            ctx.executor().execute(() -> ctx.fireExceptionCaught(e));
          } finally {
            ReferenceCountUtil.release(request);
          }
        });
  }

  private void handleFilesRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String uri, HttpMethod method) {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
      filesRequestHandler.handleUploadFileRequest(ctx, request, userToken);
    } else if (uri.startsWith("/api/files/restore") && method.equals(HttpMethod.PUT)) {
      filesRequestHandler.handleRestoreFileRequest(ctx, request, userToken);
    } else if (uri.startsWith("/api/files/list") && method.equals(HttpMethod.GET)) {
      filesRequestHandler.handleGetFileListRequest(ctx, request, userToken);
    } else if (uri.startsWith("/api/files/trash") && method.equals(HttpMethod.GET)) {
      filesRequestHandler.handleGetTrashFileListRequest(ctx, request, userToken);
    } else {
      if (uri.startsWith("/api/files/info") && method.equals(HttpMethod.GET)) {
        filesRequestHandler.handleGetFileInfoRequest(ctx, request, userToken);
      } else {
        switch (method.name()) {
          case "DELETE" -> filesRequestHandler.handleDeleteFileRequest(ctx, request, userToken);
          case "PUT" ->
              filesRequestHandler.handleChangeFileMetadataRequest(ctx, request, userToken);
          default -> ResponseUtils.sendMethodNotSupported(ctx, uri, method);
        }
      }
    }
  }

  private void handleDirectoriesRequest(
      ChannelHandlerContext ctx, HttpRequest request, String uri, HttpMethod method) {
    String userToken = extractUserTokenFromRequest(request);

    if (uri.startsWith("/api/directories") && method.equals(HttpMethod.POST)) {
      directoriesRequestHandler.handleChangeDirectoryRequest(ctx, request, userToken);
    } else {
      if (uri.startsWith("/api/directories") && method.equals(HttpMethod.PUT)) {
        directoriesRequestHandler.handleCreateDirectoryRequest(ctx, request, userToken);
      } else if (method.equals(HttpMethod.DELETE)) {
        directoriesRequestHandler.handleDeleteDirectoryRequest(ctx, request, userToken);
      } else {
        ResponseUtils.sendMethodNotSupported(ctx, uri, method);
      }
    }
  }

  private void handleUsersRequest(
      ChannelHandlerContext ctx, HttpRequest request, String uri, HttpMethod method) {
    if (method.equals(HttpMethod.POST)) {
      switch (uri) {
        case "/api/users/auth/login" -> usersRequestHandler.handleLoginRequest(ctx, request);
        case "/api/users/auth/register" -> usersRequestHandler.handleRegisterRequest(ctx, request);
        case "/api/users/auth/logout" -> usersRequestHandler.handleLogoutRequest(ctx, request);
        case "/api/users/update" -> usersRequestHandler.handleUpdateUserRequest(ctx, request);
        case "/api/users/auth/refresh" ->
            usersRequestHandler.handleRefreshTokenRequest(ctx, request);
        case "/api/users/tariff/purchase" -> usersRequestHandler.handlePurchaseTariff(ctx, request);
        case "/api/users/tariff/set-auto-renew" ->
            usersRequestHandler.handleSetAutoRenew(ctx, request);
        case "/api/users/tariff/update-payment" ->
            usersRequestHandler.handleUpdatePaymentMethod(ctx, request);
        default -> ResponseUtils.sendMethodNotSupported(ctx, uri, method);
      }
    } else if (method.equals(HttpMethod.GET)) {
      switch (uri) {
        case "/api/users/info" -> usersRequestHandler.handleGetUserRequest(ctx, request);
        case "/api/users/tariff/info" -> usersRequestHandler.handleGetTariffInfo(ctx, request);
        case "/api/users/tariff/plans" ->
            usersRequestHandler.handleGetAvailableTariffs(ctx, request);

        default -> ResponseUtils.sendMethodNotSupported(ctx, uri, method);
      }
    } else {
      ResponseUtils.sendMethodNotSupported(ctx, uri, method);
    }
  }

  private String extractUserTokenFromRequest(HttpRequest request) {
    return request.headers().get("X-Auth-Token", "");
  }
}
