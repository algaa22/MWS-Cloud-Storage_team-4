package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
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
  private static final int MAX_CHUNK_SIZE = 10 * 1024;
  private static final int MAX_AGGREGATED_SIZE = 64 * 1024;

  private final FileController fileController;
  private final UserController userController;

  public PipelineSelector(FileController fileController, UserController userController) {
    this.fileController = fileController;
    this.userController = userController;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest request) {
      String transferEncoding = request.headers().get("Transfer-Encoding");
      int contentLength = HttpUtil.getContentLength(request, -1);

      boolean useChunkedPipeline = transferEncoding.equalsIgnoreCase("chunked");

      if (useChunkedPipeline) {
        ctx.pipeline().addLast("chunkedWriter", new ChunkedWriteHandler());
        ctx.pipeline().addLast("handler", new ChunkedHttpHandler(fileController, userController));
      } else if (contentLength <= MAX_AGGREGATED_SIZE) {
        ctx.pipeline()
            .addLast(
                "aggregator",
                new HttpObjectAggregator(
                    StorageConfig.getInstance().getMaxContentLength()));
        // TODO: handler
      } else {
        handleRequestWithTooLargeContent(ctx, contentLength);
      }
    } else {
      handleNotHttpRequest(ctx, msg);
      return;
    }

    ctx.pipeline().remove(this);

    try {
      super.channelRead(ctx, msg);
    } catch (Exception e) {
      logger.error("Error during read channel in pipe selector", e);
    }
  }

  private void handleRequestWithTooLargeContent(ChannelHandlerContext ctx, int contentLength) {
    logger.error(
        "Large file with Content-Length={} not supported. Use chunked upload.", contentLength);

    ResponseHelper.sendErrorResponse(
        ctx, HttpResponseStatus.BAD_REQUEST, "Large files must use chunked upload");
  }

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
