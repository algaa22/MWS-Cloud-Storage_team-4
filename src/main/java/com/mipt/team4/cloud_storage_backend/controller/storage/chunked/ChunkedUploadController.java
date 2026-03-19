package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.S3Config;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.MissingUploadContextException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.MissingUploadPartsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooLargeFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadPartIOException;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.CreatedResponse;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartContext;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CompleteChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.CompleteUploadRetryResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.MissingUploadPartsResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.StartChunkedUploadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.UploadPartRetryResponse;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.NettyAttributes;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.ChunkedUploadService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChunkedUploadController {
  private final ChunkedUploadService uploadService;
  private final S3Config s3Config;

  public void startUploadSession(ChannelHandlerContext ctx, StartChunkedUploadRequest request) {
    StartChunkedUploadResponse response = uploadService.startChunkedUpload(request);
    ResponseUtils.send(ctx, response);
  }

  public void startPartUploading(ChannelHandlerContext ctx, ChunkedUploadPartRequest request) {
    if (uploadService.isPartAlreadyUploaded(request)) {
      ResponseUtils.send(ctx, new SuccessResponse("Part already uploaded"))
          .addListener(ChannelFutureListener.CLOSE);
      return;
    }

    cleanOldPartInfo(ctx);
    setPartContext(ctx, request);
  }

  @RequestMapping(method = "POST", path = ApiEndpoints.FILES_CHUNKED_UPLOAD)
  public void handlePartContent(ChannelHandlerContext ctx, HttpContent content) {
    ChunkedUploadPartContext currentPart = getPartAttribute(ctx).get();

    if (currentPart == null) {
      throw new MissingUploadContextException();
    }

    addChunk(ctx, currentPart, content);

    if (content instanceof LastHttpContent) {
      finalizePartUpload(ctx, currentPart);
    }
  }

  public void completeUploadSession(
      ChannelHandlerContext ctx, CompleteChunkedUploadRequest request) {
    try {
      UUID createdId = uploadService.completeChunkedUpload(request);
      ResponseUtils.send(ctx, new CreatedResponse(createdId, "File successfully uploaded"));
    } catch (UploadRetriableException e) {
      ResponseUtils.send(ctx, new CompleteUploadRetryResponse("RETRY_COMPLETE", e.getMessage()));
    } catch (MissingUploadPartsException e) {
      String bitmask = ResponseUtils.encodeBitset(e.getPartsBitSet());
      ResponseUtils.send(
          ctx, new MissingUploadPartsResponse(e.getMessage(), request.sessionId(), bitmask));
    }
  }

  private void cleanOldPartInfo(ChannelHandlerContext ctx) {
    ChunkedUploadPartContext oldPart = getPartAttribute(ctx).getAndSet(null);

    if (oldPart != null && oldPart.accumulator() != null && oldPart.accumulator().refCnt() > 0) {
      oldPart.accumulator().release();
    }
  }

  private void setPartContext(ChannelHandlerContext ctx, ChunkedUploadPartRequest request) {
    CompositeByteBuf accumulator = ctx.alloc().compositeBuffer();

    ctx.channel()
        .closeFuture()
        .addListener(
            future -> {
              if (accumulator.refCnt() > 0) {
                accumulator.release();
              }
            });

    ChunkedUploadPartContext newPart =
        new ChunkedUploadPartContext(
            request.sessionId(), request.userId(), request.part(), accumulator);
    getPartAttribute(ctx).set(newPart);
  }

  private void addChunk(
      ChannelHandlerContext ctx, ChunkedUploadPartContext currentPart, HttpContent content) {
    CompositeByteBuf accumulator = currentPart.accumulator().retain();
    ByteBuf chunk = content.content();

    try {
      if (chunk.isReadable()) {
        if (accumulator.readableBytes() + chunk.readableBytes() > s3Config.maxFilePartSize()) {
          resetCurrentPart(ctx, accumulator);
          throw new TooLargeFilePartException(s3Config.maxFilePartSize());
        }

        accumulator.addComponent(true, chunk.retain());
      }
    } finally {
      accumulator.release();
    }
  }

  private void finalizePartUpload(ChannelHandlerContext ctx, ChunkedUploadPartContext currentPart) {
    CompositeByteBuf accumulator = currentPart.accumulator();

    accumulator.retain();

    try (ByteBufInputStream inputStream = new ByteBufInputStream(accumulator)) {
      uploadService.uploadPart(
          new ChunkedUploadPartDto(
              currentPart.sessionId(),
              currentPart.userId(),
              currentPart.partNumber(),
              accumulator.readableBytes(),
              inputStream));
      ResponseUtils.send(ctx, new SuccessResponse("Part successfully uploaded"));
    } catch (IOException e) {
      throw new UploadPartIOException(ctx, e);
    } catch (UploadRetriableException e) {
      ResponseUtils.send(
          ctx, new UploadPartRetryResponse("RETRY_PART", e.getMessage(), currentPart.partNumber()));
    } finally {
      accumulator.release();
      resetCurrentPart(ctx, accumulator);
    }
  }

  private Attribute<ChunkedUploadPartContext> getPartAttribute(ChannelHandlerContext ctx) {
    return ctx.channel().attr(NettyAttributes.CHUNKED_UPLOAD_PART_INFO);
  }

  private void resetCurrentPart(ChannelHandlerContext ctx, CompositeByteBuf accumulator) {
    if (accumulator.refCnt() > 0) {
      accumulator.release();
    }

    getPartAttribute(ctx).set(null);
  }
}
