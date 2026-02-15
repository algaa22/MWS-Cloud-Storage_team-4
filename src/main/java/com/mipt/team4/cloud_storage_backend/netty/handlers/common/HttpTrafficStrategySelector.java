package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated.AggregatedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.chunked.ChunkedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
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

  private final ObjectProvider<ChunkedHttpHandler> chunkedHttpHandlerProvider;
  private final ObjectProvider<AggregatedHttpHandler> aggregatedHttpHandlerProvider;
  private final StorageConfig storageConfig;

  private PipelineType previousPipeline = null;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpObject)) {
      handleNotHttpRequest(ctx, msg);
      return;
    }

    if (msg instanceof HttpRequest request) {
      PipelineType currentPipeline;

      try {
        currentPipeline =
            PipelineType.from(request, storageConfig.rest().maxAggregatedContentLength());
      } catch (ParseException e) {
        ResponseUtils.sendBadRequestExceptionResponse(ctx, e);
        ReferenceCountUtil.release(msg);
        return;
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
      pipeline.addLast(new ChunkedWriteHandler());
      pipeline.addLast(chunkedHttpHandlerProvider.getObject());
    } else {
      pipeline.addLast(new HttpObjectAggregator(storageConfig.rest().maxAggregatedContentLength()));
      pipeline.addLast(aggregatedHttpHandlerProvider.getObject());
    }
  }

  private void safeRemoveFromPipeline(
      ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
    if (pipeline.get(handlerClass) != null) {
      pipeline.remove(handlerClass);
    }
  }

  private void handleNotHttpRequest(ChannelHandlerContext ctx, Object msg) {
    log.error(
        "Unexpected message mimeType before pipeline configuration: {}",
        msg.getClass().getSimpleName());

    ResponseUtils.sendErrorResponse(
            ctx,
            HttpResponseStatus.BAD_REQUEST,
            msg.getClass().getSimpleName() + " before pipeline configuration with HttpRequest")
        .addListener(ChannelFutureListener.CLOSE);
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
