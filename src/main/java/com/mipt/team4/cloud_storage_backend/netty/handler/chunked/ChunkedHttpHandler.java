package com.mipt.team4.cloud_storage_backend.netty.handler.chunked;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
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

  // TODO: При обрыве соединения — загрузка прерывается, файл остаётся недозагруженным?
  //       (resume-upload)

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    try {
      if (msg instanceof HttpRequest request) {
        handleHttpRequest(ctx, request);
      } else if (msg instanceof HttpContent content) {
        handleHttpContent(ctx, content);
      }
    } catch (StorageFileAlreadyExistsException
        | UserNotFoundException
        | UploadSessionNotFoundException
        | HeaderNotFoundException
        | TransferAlreadyStartedException
        | TransferNotStartedYetException
        | StorageFileNotFoundException
        | TooSmallFilePartException
        | ValidationFailedException
        | StorageIllegalAccessException
        | QueryParameterNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
    } catch (CombineChunksToPartException | MissingFilePartException e) {
      ResponseHelper.sendInternalServerErrorResponse(ctx);
      logger.error("Internal server error: {}", e.getMessage());
    }
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          QueryParameterNotFoundException,
          HeaderNotFoundException,
          TransferAlreadyStartedException {
    startChunkedTransfer(ctx, request);
  }

  private void startChunkedTransfer(ChannelHandlerContext ctx, HttpRequest request)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          QueryParameterNotFoundException,
          HeaderNotFoundException,
          TransferAlreadyStartedException,
          StorageFileNotFoundException {
    String uri = request.uri();
    HttpMethod method = request.method();

    if (uri.startsWith("/api/files/upload") && method.equals(HttpMethod.POST)) {
      chunkedUpload.startChunkedUpload(request);
    } else if (uri.startsWith("/api/files") && method.equals(HttpMethod.GET)) {
      chunkedDownload.startChunkedDownload(ctx, request);
    } else {
      ResponseHelper.sendMethodNotSupportedResponse(ctx, uri, method);
    }
  }

  private void handleHttpContent(ChannelHandlerContext ctx, HttpContent content)
      throws UserNotFoundException,
          StorageFileAlreadyExistsException,
          TooSmallFilePartException,
          UploadSessionNotFoundException,
          CombineChunksToPartException,
          ValidationFailedException,
          MissingFilePartException,
          TransferNotStartedYetException {
    if (content instanceof LastHttpContent) {
      chunkedUpload.completeChunkedUpload(ctx, (LastHttpContent) content);
    } else {
      chunkedUpload.handleFileChunk(ctx, content);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.error("Unhandled exception in channel from {}", ctx.channel().remoteAddress(), cause);
    ResponseHelper.sendInternalServerErrorResponse(ctx);

    chunkedUpload.cleanup();
    chunkedDownload.cleanup();
  }
}
