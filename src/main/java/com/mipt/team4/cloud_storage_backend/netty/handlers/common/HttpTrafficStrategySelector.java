package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.netty.NotHttpRequestException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import com.mipt.team4.cloud_storage_backend.netty.handlers.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated.AggregatedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.chunked.ChunkedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
@RequiredArgsConstructor
public class HttpTrafficStrategySelector extends ChannelInboundHandlerAdapter {

  private final ObjectProvider<ChunkedHttpHandler> chunkedHttpHandlers;
  private final ObjectProvider<AggregatedHttpHandler> aggregatedHttpHandlerProvider;
  private final StorageConfig storageConfig;

  private PipelineType previousPipeline = null;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpObject)) {
      ReferenceCountUtil.release(msg);
      throw new NotHttpRequestException();
    }

    if (msg instanceof HttpRequest request) {
      PipelineType currentPipeline;

      try {
        currentPipeline =
            PipelineType.from(request, storageConfig.rest().maxAggregatedContentLength());
      } catch (ParseException e) {
        ReferenceCountUtil.release(msg);
        throw e;
      }

      if (previousPipeline != currentPipeline) {
        cleanupPipeline(ctx);
        configurePipeline(currentPipeline, ctx);

        previousPipeline = currentPipeline;
      }
    }

    ctx.fireChannelRead(msg);
  }

  private void cleanupPipeline(ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (previousPipeline == PipelineType.CHUNKED) {
      safeRemoveFromPipeline(pipeline, ChunkedWriteHandler.class);
      safeRemoveFromPipeline(pipeline, ChunkedHttpHandler.class);
    }

    if (previousPipeline == PipelineType.AGGREGATED) {
      safeRemoveFromPipeline(pipeline, HttpObjectAggregator.class);
      safeRemoveFromPipeline(pipeline, AggregatedHttpHandler.class);
    }
  }

  private void configurePipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (currentPipeline == PipelineType.CHUNKED) {
      addHandlerToPipeline(pipeline, PipelineHandlerNames.CHUNKED_WRITE, new ChunkedWriteHandler());
      addHandlerToPipeline(
          pipeline, PipelineHandlerNames.CHUNKED_HTTP, chunkedHttpHandlers.getObject());
    } else {
      addHandlerToPipeline(
          pipeline,
          PipelineHandlerNames.HTTP_OBJECT_AGGREGATOR,
          new HttpObjectAggregator(storageConfig.rest().maxAggregatedContentLength()));
      addHandlerToPipeline(
          pipeline,
          PipelineHandlerNames.AGGREGATED_HTTP,
          aggregatedHttpHandlerProvider.getObject());
    }
  }

  private void addHandlerToPipeline(ChannelPipeline pipeline, String name, ChannelHandler handler) {
    pipeline.addBefore(PipelineHandlerNames.STORAGE_EXCEPTION, name, handler);
  }

  private void safeRemoveFromPipeline(
      ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
    if (pipeline.get(handlerClass) != null) {
      pipeline.remove(handlerClass);
    }
  }

  public enum PipelineType {
    CHUNKED,
    AGGREGATED;

    public static PipelineType from(HttpRequest request, int maxAggregatedContentLength)
        throws ParseException {
      if (request.method() == HttpMethod.POST) {
        int fileSize =
            SafeParser.parseInt("File size", RequestUtils.getHeader(request, "X-File-Size", "0"));

        if (fileSize > maxAggregatedContentLength) {
          return CHUNKED;
        }
      }

      if (request.method() == HttpMethod.GET && request.uri().startsWith("/api/files/download")) {
        return CHUNKED;
      }

      return AGGREGATED;
    }
  }
}
