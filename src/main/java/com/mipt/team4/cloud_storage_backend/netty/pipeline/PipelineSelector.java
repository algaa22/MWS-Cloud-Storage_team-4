package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handler.AggregatedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.handler.ChunkedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class PipelineSelector extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(PipelineSelector.class);

  private final FileController fileController;
  private final UserController userController;

  private PipelineType previousPipeline = null;

  public PipelineSelector(FileController fileController, UserController userController) {
    this.fileController = fileController;
    this.userController = userController;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof HttpObject)) {
      handleNotHttpRequest(ctx, msg);
      return;
    }

    if (msg instanceof HttpRequest request) {
      // TODO: защита от атак (валидаторы)
      PipelineType currentPipeline = PipelineType.from(request);

      cleanupPipeline(currentPipeline, ctx);
      configurePipeline(currentPipeline, ctx);

      previousPipeline = currentPipeline;
    }

    ctx.fireChannelRead(msg);
  }

  private void cleanupPipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    if (previousPipeline == currentPipeline) return;

    ChannelPipeline pipeline = ctx.pipeline();

    if (previousPipeline == PipelineType.CHUNKED) {
      pipeline.remove(ChunkedWriteHandler.class);
      pipeline.remove(ChunkedHttpHandler.class);
    }

    if (previousPipeline == PipelineType.AGGREGATED) {
      pipeline.remove(HttpObjectAggregator.class);
      pipeline.remove(AggregatedHttpHandler.class);
    }
  }

  private void configurePipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (currentPipeline == PipelineType.CHUNKED) {
      pipeline.addLast(new ChunkedWriteHandler());
      pipeline.addLast(new ChunkedHttpHandler(fileController));
    } else {
      pipeline.addLast(new HttpObjectAggregator(StorageConfig.INSTANCE.getMaxContentLength()));
      pipeline.addLast(new AggregatedHttpHandler(fileController, userController));
    }
  }

  //  private void handleRequestWithTooLargeContent(ChannelHandlerContext ctx, int contentLength) {
  //    logger.error(
  //        "Large file with Content-Length={} not supported. Use chunked upload.", contentLength);
  //
  //    ResponseHelper.sendErrorResponse(
  //        ctx, HttpResponseStatus.BAD_REQUEST, "Large files must use chunked upload");
  //  }

  private void handleNotHttpRequest(ChannelHandlerContext ctx, Object msg) {
    logger.error(
        "Unexpected message type before pipeline configuration: {}",
        msg.getClass().getSimpleName());

    ResponseHelper.sendErrorResponse(
            ctx,
            HttpResponseStatus.BAD_REQUEST,
            msg.getClass().getSimpleName() + " before pipeline configuration with HttpRequest")
        .addListener(ChannelFutureListener.CLOSE);
  }
}
