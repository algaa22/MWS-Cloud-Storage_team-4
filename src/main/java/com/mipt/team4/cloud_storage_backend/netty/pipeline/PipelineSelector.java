package com.mipt.team4.cloud_storage_backend.netty.pipeline;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handler.ChunkedHttpHandler;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineSelector extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(PipelineSelector.class);
  // TODO: настроить и возможно в кфг
  private static final int MAX_AGGREGATED_SIZE = 64 * 1024;

  private final FileController fileController;
  private final UserController userController;

  private PipelineType previousPipeline = null;

  public PipelineSelector(FileController fileController, UserController userController) {
    this.fileController = fileController;
    this.userController = userController;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest request) {
      // TODO: защита от атак (валидаторы)
      PipelineType currentPipeline = PipelineType.from(request);

      cleanupPipeline(currentPipeline, ctx);
      configurePipeline(currentPipeline, ctx);

      previousPipeline = currentPipeline;
    } else {
      handleNotHttpRequest(ctx, msg);
      return;
    }

    ctx.fireChannelRead(msg);
  }

  private void cleanupPipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    if (previousPipeline == currentPipeline) return;

    if (previousPipeline == PipelineType.CHUNKED) {
      ctx.pipeline().remove(ChunkedWriteHandler.class);
      ctx.pipeline().remove(ChunkedHttpHandler.class);
    }

    if (previousPipeline == PipelineType.AGGREGATED) {
      ctx.pipeline().remove(HttpObjectAggregator.class);
      // handler
    }
  }

  private void configurePipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    if (currentPipeline == PipelineType.CHUNKED) {
      ctx.pipeline().addLast(new ChunkedWriteHandler());
      ctx.pipeline().addLast(new ChunkedHttpHandler(fileController));
    } else {
      ctx.pipeline().addLast(new HttpObjectAggregator(StorageConfig.getInstance().getMaxContentLength()));
      // TODO: handler
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
