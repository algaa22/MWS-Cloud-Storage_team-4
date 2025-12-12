package com.mipt.team4.cloud_storage_backend.netty.handler.filters;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.pipeline.PipelineSelector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http2RequestFilter extends ApplicationProtocolNegotiationHandler {
  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  public Http2RequestFilter(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;
  }

  // TODO: дублирование с NettyServer.initChannel
  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
    // TODO: pipeline = ctx.pipeline()
    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());

      // TODO: как это работает
      ChannelInitializer<Channel> streamInitializer = new ChannelInitializer<>() {
        @Override
        protected void initChannel(Channel ch) {
          ch.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(true));
          ch.pipeline().addLast(new CorsFilter());
          ch.pipeline().addLast(new PipelineSelector(fileController, directoryController, userController));
        }
      };

      ctx.pipeline().addLast(new Http2MultiplexHandler(streamInitializer));
    } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      ctx.pipeline().addLast(new HttpServerCodec());
      ctx.pipeline().addLast(new CorsFilter());
      ctx.pipeline()
              .addLast(new PipelineSelector(fileController, directoryController, userController));
    } else {
      ctx.channel().close();
    }
  }
}
