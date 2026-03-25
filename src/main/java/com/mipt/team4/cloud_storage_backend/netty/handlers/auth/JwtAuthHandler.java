package com.mipt.team4.cloud_storage_backend.netty.handlers.auth;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.MissingAuthTokenException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserSessionDto;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.NettyAttributes;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class JwtAuthHandler extends ChannelInboundHandlerAdapter {
  private final UserSessionService userSessionService;

  private static final Set<String> AUTH_WHITELIST =
      Set.of(
          ApiEndpoints.AUTH_REGISTER,
          ApiEndpoints.AUTH_LOGIN,
          ApiEndpoints.AUTH_REFRESH,
          ApiEndpoints.HEALTHCHECK);

  private static final String AUTH_HEADER = "X-Auth-Token";

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpRequest request)) {
      ctx.fireChannelRead(msg);
      return;
    }

    String path = request.uri().split("\\?")[0];

    if (AUTH_WHITELIST.contains(path)) {
      ctx.fireChannelRead(request);
      return;
    }

    String token = request.headers().get(AUTH_HEADER);
    if (token == null || token.isBlank()) {
      throw new MissingAuthTokenException();
    }

    Optional<UserSessionDto> sessionDto = userSessionService.getSession(token);
    if (sessionDto.isEmpty()) {
      throw new InvalidSessionException();
    }

    UUID userId = sessionDto.get().userId();
    ctx.channel().attr(NettyAttributes.USER_ID).set(userId);
    ctx.fireChannelRead(request);
  }
}
