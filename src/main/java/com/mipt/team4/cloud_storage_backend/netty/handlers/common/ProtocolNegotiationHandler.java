  package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

  import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
  import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
  import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
  import com.mipt.team4.cloud_storage_backend.netty.channel.Http2StreamInitializer;
  import com.mipt.team4.cloud_storage_backend.netty.handlers.common.HttpTrafficStrategySelector.PipelineType;
  import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineUtils;
  import io.netty.channel.ChannelHandlerContext;
  import io.netty.channel.ChannelPipeline;
  import io.netty.handler.codec.http.HttpServerCodec;
  import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
  import io.netty.handler.codec.http2.Http2MultiplexHandler;
  import io.netty.handler.ssl.ApplicationProtocolNames;
  import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

  public class ProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {

    private final FileController fileController;
    private final DirectoryController directoryController;
    private final UserController userController;

    public ProtocolNegotiationHandler(
        FileController fileController,
        DirectoryController directoryController,
        UserController userController) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.fileController = fileController;
      this.directoryController = directoryController;
      this.userController = userController;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
      ChannelPipeline pipeline = ctx.pipeline();

      if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        pipeline.addLast(Http2FrameCodecBuilder.forServer().build());
        pipeline.addLast(new Http2MultiplexHandler(
            new Http2StreamInitializer(fileController, directoryController, userController)));
      } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        PipelineUtils.buildHttp11Pipeline(pipeline, fileController, directoryController, userController);
      } else {
        ctx.channel().close();
      }
    }
  }
