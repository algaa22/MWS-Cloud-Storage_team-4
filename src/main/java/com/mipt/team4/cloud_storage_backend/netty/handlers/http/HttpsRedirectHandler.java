package com.mipt.team4.cloud_storage_backend.netty.handlers.http;

import com.mipt.team4.cloud_storage_backend.config.props.NettyProps;
import com.mipt.team4.cloud_storage_backend.exception.netty.MissingHostHeaderException;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class HttpsRedirectHandler extends ChannelInboundHandlerAdapter {
  private final NettyProps nettyConfig;

  private static final Set<String> REDIRECT_WHITELIST = Set.of(ApiEndpoints.HEALTHCHECK);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!nettyConfig.httpsRedirect() || !nettyConfig.enableHttps()) {
      ctx.fireChannelRead(msg);
      return;
    }

    if (!(msg instanceof HttpRequest request)) {
      ctx.fireChannelRead(msg);
      return;
    }

    String path = request.uri().split("\\?")[0];

    if (REDIRECT_WHITELIST.contains(path)) {
      ctx.fireChannelRead(msg);
      return;
    }

    try {
      String host = request.headers().get(HttpHeaderNames.HOST);

      if (host == null) {
        throw new MissingHostHeaderException();
      }

      String newHost = host.split(":")[0] + ":" + nettyConfig.httpsPort();
      String newUrl = "https://" + newHost + request.uri();

      FullHttpResponse response =
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);

      response.headers().set(HttpHeaderNames.LOCATION, newUrl);
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

      ResponseUtils.send(ctx, response).addListener(ChannelFutureListener.CLOSE);
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }
}
