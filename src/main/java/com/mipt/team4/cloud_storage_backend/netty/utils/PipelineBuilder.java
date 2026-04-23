package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.mipt.team4.cloud_storage_backend.netty.constants.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.auth.JwtAuthHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.error.GlobalErrorHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.error.StorageExceptionHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.http.CorsHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.http.HttpsRedirectHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.http.IdleTimeoutHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.rest.HttpTrafficStrategySelector;
import com.mipt.team4.cloud_storage_backend.netty.mapping.codec.DtoToResponseEncoder;
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
  private final GlobalErrorHandler globalErrorHandlers;
  private final IdleTimeoutHandler idleTimeoutHandlers;
  private final HttpsRedirectHandler httpsRedirectHandler;
  private final DtoToResponseEncoder dtoToResponseEncoder;
  private final JwtAuthHandler jwtAuthHandler;
  private final CorsHandler corsHandler;

  public void buildHttp11Pipeline(ChannelPipeline pipeline) {
    pipeline.addLast(PipelineHandlerNames.HTTP_SERVER_CODEC, new HttpServerCodec());
    pipeline.addLast(PipelineHandlerNames.HTTPS_REDIRECT, httpsRedirectHandler);
    finalizePipeline(pipeline);
  }

  public void finalizePipeline(ChannelPipeline pipeline) {
    pipeline.addLast(PipelineHandlerNames.HEAD_GLOBAL_ERROR, globalErrorHandlers);
    pipeline.addLast(PipelineHandlerNames.CORS, corsHandler);
    pipeline.addLast(PipelineHandlerNames.DTO_TO_RESPONSE, dtoToResponseEncoder);
    pipeline.addLast(PipelineHandlerNames.TRAFFIC_STRATEGY_SELECTOR, strategySelectors.getObject());
    pipeline.addLast(PipelineHandlerNames.JWT_AUTH, jwtAuthHandler);

    pipeline.addLast(PipelineHandlerNames.STORAGE_EXCEPTION, storageExceptionHandlers);
    pipeline.addLast(PipelineHandlerNames.TAIL_GLOBAL_ERROR, globalErrorHandlers);
    pipeline.addLast(PipelineHandlerNames.IDLE_TIMEOUT, idleTimeoutHandlers);
  }
}
