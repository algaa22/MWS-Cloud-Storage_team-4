package com.mipt.team4.cloud_storage_backend.netty.handlers.auth;

import com.mipt.team4.cloud_storage_backend.exception.user.auth.InvalidTokenException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.MissingAuthTokenException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenClaimsDto;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.SecurityAttributes;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class JwtAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final AccessTokenService accessTokenService;

  private static final Set<String> AUTH_WHITELIST =
      Set.of(ApiEndpoints.AUTH_REGISTER, ApiEndpoints.AUTH_LOGIN);

  private static final String AUTH_HEADER = "X-Auth-Token";

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
    String path = request.uri().split("\\?")[0];

    if (AUTH_WHITELIST.contains(path)) {
      ctx.fireChannelRead(request.retain());
      return;
    }

    String token = request.headers().get(AUTH_HEADER);
    if (token == null || token.isBlank()) {
      throw new MissingAuthTokenException();
    }

    TokenClaimsDto claims = accessTokenService.extractTokenClaims(token);
    if (!claims.isValid()) {
      throw new InvalidTokenException();
    }

    ctx.channel().attr(SecurityAttributes.USER_ID).set(claims.userId());
    ctx.fireChannelRead(request.retain());
  }
}
