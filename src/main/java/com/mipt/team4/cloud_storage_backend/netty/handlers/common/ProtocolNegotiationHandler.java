package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.netty.channel.Http2StreamInitializer;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import org.springframework.stereotype.Component;

@Component
public class ProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {
  private final Http2StreamInitializer http2StreamInitializer;
  private final PipelineBuilder pipelineBuilder;

  public ProtocolNegotiationHandler(
      Http2StreamInitializer http2StreamInitializer, PipelineBuilder pipelineBuilder) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.http2StreamInitializer = http2StreamInitializer;
    this.pipelineBuilder = pipelineBuilder;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      pipeline.addLast(Http2FrameCodecBuilder.forServer().build());
      pipeline.addLast(new Http2MultiplexHandler(http2StreamInitializer));
    } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      pipelineBuilder.buildHttp11Pipeline(pipeline);
    } else {
      ctx.channel().close();
    }
  }
}
