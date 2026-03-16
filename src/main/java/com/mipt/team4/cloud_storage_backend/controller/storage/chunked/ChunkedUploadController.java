package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferAlreadyStartedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TransferNotStartedYetException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadNotStoppedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResult;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ResumeChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.UploadRetryResponse;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class ChunkedUploadController {
  private final FileService fileService;

  private StartChunkedUploadRequest metadata;
  private boolean isInProgress = false;

  public void start(ChannelHandlerContext ctx, StartChunkedUploadRequest request) {
    if (isInProgress) {
      throw new TransferAlreadyStartedException();
    }

    fileService.startChunkedUpload(request);

    isInProgress = true;
    metadata = request;
  }

  public void resume(ChannelHandlerContext ctx, ResumeChunkedUploadDto request) {
    if (isInProgress) {
      throw new UploadNotStoppedException();
    }

    fileService.resumeChunkedUploadSession(request);
    isInProgress = true;
  }

  public void handleChunk(ChannelHandlerContext ctx, HttpContent content) {
    if (!isInProgress) {
      throw new TransferNotStartedYetException();
    }

    ByteBuf data = content.content();
    byte[] bytes = new byte[data.readableBytes()];
    data.readBytes(bytes);

    try {
      fileService.uploadChunk(new UploadChunkDto(metadata.sessionId(), bytes));
    } catch (ProcessUploadRetriableException e) {
      ResponseUtils.send(ctx, new UploadRetryResponse(e));
    }
  }

  public void complete(ChannelHandlerContext ctx, LastHttpContent content) {
    if (!isInProgress) throw new TransferNotStartedYetException();

    if (content.content().readableBytes() > 0) {
      handleChunk(ctx, content);
    }

    try {
      ChunkedUploadFileResult result = fileService.completeChunkedUpload(metadata.sessionId());
      ctx.writeAndFlush(result);
      cleanup();
    } catch (CompleteUploadRetriableException e) {
      ResponseUtils.send(ctx, new UploadRetryResponse(e));
    }
  }

  public void cleanup() {
    isInProgress = false;
    metadata = null;
  }
}
