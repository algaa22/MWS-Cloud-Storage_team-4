package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.mipt.team4.cloud_storage_backend.netty.handlers.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.CorsHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.IdleTimeoutHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.error.FinalErrorHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.error.StorageExceptionHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.rest.HttpTrafficStrategySelector;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineBuilder {
  private final ObjectProvider<HttpTrafficStrategySelector> strategySelectors;
  private final StorageExceptionHandler storageExceptionHandlers;
  private final FinalErrorHandler globalErrorHandlers;
  private final IdleTimeoutHandler idleTimeoutHandlers;
  private final CorsHandler corsHandler;

  public void buildHttp11Pipeline(ChannelPipeline pipeline) {
    pipeline.addLast(PipelineHandlerNames.HTTP_SERVER_CODEC, new HttpServerCodec());
    finalizeHttpPipeline(pipeline);
  }

  public void finalizeHttpPipeline(ChannelPipeline pipeline) {
    pipeline.addLast(PipelineHandlerNames.CORS, corsHandler);
    pipeline.addLast(PipelineHandlerNames.TRAFFIC_STRATEGY_SELECTOR, strategySelectors.getObject());

    pipeline.addLast(PipelineHandlerNames.STORAGE_EXCEPTION, storageExceptionHandlers);
    pipeline.addLast(PipelineHandlerNames.GLOBAL_ERROR, globalErrorHandlers);
    pipeline.addLast(PipelineHandlerNames.IDLE_TIMEOUT, idleTimeoutHandlers);
  }
}
