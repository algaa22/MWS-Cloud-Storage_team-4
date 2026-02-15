package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Http2StreamInitializer extends ChannelInitializer<Channel> {
  private final PipelineBuilder pipelineBuilder;

  @Override
  protected void initChannel(Channel channel) {
    ChannelPipeline pipeline = channel.pipeline();

    pipeline.addLast(new Http2StreamFrameToHttpObjectCodec(true));
    pipelineBuilder.finalizeHttpPipeline(pipeline);
  }
}
