package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.mipt.team4.cloud_storage_backend.netty.handlers.common.CorsHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.HttpTrafficStrategySelector;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class PipelineBuilder {
  private final ObjectProvider<HttpTrafficStrategySelector> selectorProvider;
  private final ObjectProvider<CorsHandler> corsHandler;

  public PipelineBuilder(
      ObjectProvider<HttpTrafficStrategySelector> selectorProvider,
      ObjectProvider<CorsHandler> corsHandler) {
    this.selectorProvider = selectorProvider;
    this.corsHandler = corsHandler;
  }

  public void buildHttp11Pipeline(ChannelPipeline pipeline) {
    pipeline.addLast(new HttpServerCodec());
    finalizeHttpPipeline(pipeline);
  }

  public void finalizeHttpPipeline(ChannelPipeline pipeline) {
    pipeline.addLast(corsHandler.getObject());
    pipeline.addLast(selectorProvider.getObject());
  }
}
