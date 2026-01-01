package com.mipt.team4.cloud_storage_backend.netty.handlers.common;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTrafficStrategySelector extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(HttpTrafficStrategySelector.class);

  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  private PipelineType previousPipeline = null;

  public enum PipelineType {
    CHUNKED,
    AGGREGATED;

    public static PipelineType from(HttpRequest request) throws ParseException {
      if (request.method() == HttpMethod.POST) {
        int fileSize =
            SafeParser.parseInt("File size", RequestUtils.getHeader(request, "X-File-Size", "0"));

        if (fileSize > StorageConfig.INSTANCE.getMaxAggregatedContentLength()) {
          return CHUNKED;
        }
      }

      if (request.method() == HttpMethod.GET && request.uri().startsWith("/api/files/download")) {
        return CHUNKED;
      }

      return AGGREGATED;
    }
  }

  public HttpTrafficStrategySelector(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpObject)) {
      handleNotHttpRequest(ctx, msg);
      return;
    }

    if (msg instanceof HttpRequest request) {
      PipelineType currentPipeline;

      try {
        currentPipeline = PipelineType.from(request);
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
      pipeline.addLast(new ChunkedHttpHandler(fileController));
    } else {
      pipeline.addLast(
          new HttpObjectAggregator(StorageConfig.INSTANCE.getMaxAggregatedContentLength()));
      pipeline.addLast(
          new AggregatedHttpHandler(fileController, directoryController, userController));
    }
  }

  private void safeRemoveFromPipeline(
      ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
    if (pipeline.get(handlerClass) != null) {
      pipeline.remove(handlerClass);
    }
  }

  private void handleNotHttpRequest(ChannelHandlerContext ctx, Object msg) {
    logger.error(
        "Unexpected message mimeType before pipeline configuration: {}",
        msg.getClass().getSimpleName());

    ResponseUtils.sendErrorResponse(
            ctx,
            HttpResponseStatus.BAD_REQUEST,
            msg.getClass().getSimpleName() + " before pipeline configuration with HttpRequest")
        .addListener(ChannelFutureListener.CLOSE);
  }
}
