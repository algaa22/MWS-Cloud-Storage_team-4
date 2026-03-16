package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.IncorrectChunkedUploadStateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ResumeChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.UploadRetryResponse;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
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

  private ChunkedUploadInfoDto uploadInfo;
  private State state = State.IDLE;

  public void start(ChannelHandlerContext ctx, StartChunkedUploadRequest request) {
    if (state != State.IDLE) {
      throw new IncorrectChunkedUploadStateException(State.IDLE, state);
    }

    uploadInfo = fileService.startChunkedUpload(request);
    state = State.PROCESSING;
  }

  public void resume(ChannelHandlerContext ctx, ResumeChunkedUploadRequest request) {
    if (state != State.STOPPED) {
      throw new IncorrectChunkedUploadStateException(State.STOPPED, state);
    }

    fileService.resumeChunkedUploadSession(uploadInfo);
    state = State.PROCESSING;
  }

  @RequestMapping(method = "POST", path = ApiEndpoints.FILES_CHUNKED_UPLOAD)
  public void handleContent(ChannelHandlerContext ctx, HttpContent content) {
    if (content instanceof LastHttpContent lastContent) {
      complete(ctx, lastContent);
      return;
    }

    handleChunk(ctx, content);
  }

  private void handleChunk(ChannelHandlerContext ctx, HttpContent content) {
    if (state != State.PROCESSING) {
      throw new IncorrectChunkedUploadStateException(State.PROCESSING, state);
    }

    ByteBuf data = content.content();
    byte[] bytes = new byte[data.readableBytes()];
    data.readBytes(bytes);

    try {
      fileService.uploadChunk(new UploadChunkDto(uploadInfo.sessionId(), bytes));
    } catch (ProcessUploadRetriableException e) {
      ResponseUtils.send(ctx, new UploadRetryResponse(e));
      state = State.STOPPED;
    }
  }

  public void complete(ChannelHandlerContext ctx, LastHttpContent content) {
    if (state != State.PROCESSING) {
      throw new IncorrectChunkedUploadStateException(State.PROCESSING, state);
    }

    if (content.content().readableBytes() > 0) {
      handleChunk(ctx, content);
    }

    try {
      ChunkedUploadFileResponse result = fileService.completeChunkedUpload(uploadInfo);
      ResponseUtils.send(ctx, result);
      state = State.COMPLETED;
    } catch (CompleteUploadRetriableException e) {
      ResponseUtils.send(ctx, new UploadRetryResponse(e));
      state = State.STOPPED;
    }
  }

  public enum State {
    IDLE,
    PROCESSING,
    STOPPED,
    COMPLETED
  }
}
