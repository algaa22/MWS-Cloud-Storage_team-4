package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
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

      boolean isExplicitChunked = request.headers().contains("X-Chunked-Upload");
      boolean isHttpChunked = transferEncoding.equalsIgnoreCase("chunked");
      boolean useChunkedPipeline = isExplicitChunked || isHttpChunked;

      if (useChunkedPipeline && contentLength <= MAX_CHUNK_SIZE) {
        addChunkedWrite(ctx);
        addChunkedHandler(ctx);
      } else if (!useChunkedPipeline && contentLength <= MAX_AGGREGATED_SIZE) {
        addAggregator(ctx);
        // TODO: handler
      } else {
        handleRequestWithTooLargeContent(ctx, contentLength);
      }
    } else {
      addChunkedWrite(ctx);
    }

    finishSelection(ctx, msg);
  }

  private void addChunkedHandler(ChannelHandlerContext ctx) {
    // TODO
  }

  private void handleRequestWithTooLargeContent(ChannelHandlerContext ctx, int contentLength) {
    logger.error(
        "Large file with Content-Length={} not supported. Use chunked upload.", contentLength);

    ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Large files must use chunked upload");
  }

  private void addChunkedWrite(ChannelHandlerContext ctx) {
    ctx.pipeline().addLast("chunkedWriter", new ChunkedWriteHandler());
  }

  private void addAggregator(ChannelHandlerContext ctx) {
    ctx.pipeline().addLast("aggregator", new HttpObjectAggregator(65536)); // TODO: hard coding
  }

  private void addHandler(ChannelHandlerContext ctx) {
    ctx.pipeline().addLast("handler", new HttpRequestHandler(fileController, userController));
  }

  private void finishSelection(ChannelHandlerContext ctx, Object msg) {
    ctx.pipeline().remove(this);

    try {
      super.channelRead(ctx, msg);
    } catch (Exception e) {
      logger.error("Error during read channel in pipe selector", e);
    }
  }
}
