package com.mipt.team4.cloud_storage_backend.netty.handlers.rest.aggregated;

import com.mipt.team4.cloud_storage_backend.exception.netty.NotHttpRequestException;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
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

            if (uri.startsWith(ApiEndpoints.FILES_PREFIX)) {
              handleFilesRequest(ctx, request, uri, method);
            } else if (uri.startsWith(ApiEndpoints.DIRECTORIES_PREFIX)) {
              handleDirectoriesRequest(ctx, request, uri, method);
            } else if (uri.startsWith(ApiEndpoints.USERS_PREFIX)) {
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

    if (uri.startsWith(ApiEndpoints.FILES_UPLOAD) && method.equals(HttpMethod.POST)) {
      filesRequestHandler.handleUploadFileRequest(ctx, request, userToken);
    } else if (uri.startsWith(ApiEndpoints.FILES_RESTORE) && method.equals(HttpMethod.PUT)) {
      filesRequestHandler.handleRestoreFileRequest(ctx, request, userToken);
    } else if (uri.startsWith(ApiEndpoints.FILES_LIST) && method.equals(HttpMethod.GET)) {
      filesRequestHandler.handleGetFileListRequest(ctx, request, userToken);
    } else if (uri.startsWith(ApiEndpoints.FILES_TRASH) && method.equals(HttpMethod.GET)) {
      filesRequestHandler.handleGetTrashFileListRequest(ctx, request, userToken);
    } else {
      if (uri.startsWith(ApiEndpoints.FILES_INFO) && method.equals(HttpMethod.GET)) {
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

    if (uri.startsWith(ApiEndpoints.DIRECTORIES_ROOT) && method.equals(HttpMethod.POST)) {
      directoriesRequestHandler.handleChangeDirectoryRequest(ctx, request, userToken);
    } else {
      if (uri.startsWith(ApiEndpoints.DIRECTORIES_ROOT) && method.equals(HttpMethod.PUT)) {
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
        case ApiEndpoints.AUTH_LOGIN -> usersRequestHandler.handleLoginRequest(ctx, request);
        case ApiEndpoints.AUTH_REGISTER -> usersRequestHandler.handleRegisterRequest(ctx, request);
        case ApiEndpoints.AUTH_LOGOUT -> usersRequestHandler.handleLogoutRequest(ctx, request);
        case ApiEndpoints.USERS_UPDATE -> usersRequestHandler.handleUpdateUserRequest(ctx, request);
        case ApiEndpoints.AUTH_REFRESH ->
            usersRequestHandler.handleRefreshTokenRequest(ctx, request);
        case ApiEndpoints.TARIFF_PURCHASE -> usersRequestHandler.handlePurchaseTariff(ctx, request);
        case ApiEndpoints.TARIFF_SET_AUTO_RENEW ->
            usersRequestHandler.handleSetAutoRenew(ctx, request);
        case ApiEndpoints.TARIFF_UPDATE_PAYMENT ->
            usersRequestHandler.handleUpdatePaymentMethod(ctx, request);
        default -> ResponseUtils.sendMethodNotSupported(ctx, uri, method);
      }
    } else if (method.equals(HttpMethod.GET)) {
      switch (uri) {
        case ApiEndpoints.USERS_INFO -> usersRequestHandler.handleGetUserRequest(ctx, request);
        case ApiEndpoints.TARIFF_INFO -> usersRequestHandler.handleGetTariffInfo(ctx, request);
        case ApiEndpoints.TARIFF_PLANS ->
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
