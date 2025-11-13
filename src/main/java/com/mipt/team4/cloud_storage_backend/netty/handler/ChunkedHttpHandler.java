package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkedHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
  private static final Logger logger = LoggerFactory.getLogger(ChunkedHttpHandler.class);
  private final ChunkedUploadHandler chunkedUpload;
  private final ChunkedDownloadHandler chunkedDownload;

  public ChunkedHttpHandler(FileController fileController) {
    this.chunkedUpload = new ChunkedUploadHandler(fileController);
    this.chunkedDownload = new ChunkedDownloadHandler(fileController);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof HttpRequest request) {
      handleHttpRequest(ctx, request);
    } else if (msg instanceof HttpContent content) {
      handleHttpContent(ctx, content);
    }
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
    startChunkedTransfer(ctx, request);
  }

  private void startChunkedTransfer(ChannelHandlerContext ctx, HttpRequest request) {
    String uri = request.uri();
    HttpMethod method = request.method();

    try {
      if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
        chunkedUpload.startChunkedUpload(ctx, request);
      } else if (uri.startsWith("/api/files/") && method.equals(HttpMethod.GET)) {
        chunkedDownload.startChunkedDownload(ctx, request);
      } else {
        ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
      }
    } catch (TransferAlreadyStartedException e) {
      handleBadRequest(
          ctx,
          "New HttpRequest received while previous request is in progress",
          "Previous request not completed");
    }
  }

  private void handleHttpContent(ChannelHandlerContext ctx, HttpContent content) {
    try {
      if (content instanceof LastHttpContent) {
        chunkedUpload.finishChunkedUpload(ctx, (LastHttpContent) content);
      } else {
        chunkedUpload.handleFileChunk(ctx, content);
      }
    } catch (TransferNotStartedYetException e) {
      handleTransferNotStartedYet(ctx);
    }
  }

  private void handleTransferNotStartedYet(ChannelHandlerContext ctx) {
    handleBadRequest(
        ctx, "HttpContent received without active HttpRequest", "HTTP content without request");
  }

  private void handleBadRequest(
      ChannelHandlerContext ctx, String loggerMessage, String responseMessage) {
    logger.error(loggerMessage);

    ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, responseMessage);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unhandled exception in channel from {}", ctx.channel().remoteAddress(), cause);
    ResponseHelper.sendInternalServerErrorResponse(ctx);

    chunkedUpload.cleanup();
    chunkedDownload.cleanup();
  }
}
