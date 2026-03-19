package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.exception.transfer.MissingUploadContextException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadPartIOException;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.CreatedResponse;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartContext;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CompleteChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.ChunkedUploadInfoResponse;
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

  public void start(ChannelHandlerContext ctx, StartChunkedUploadRequest request) {
    ChunkedUploadInfoResponse response = uploadService.startChunkedUpload(request);
    ResponseUtils.send(ctx, response);
  }

  public void uploadPart(ChannelHandlerContext ctx, ChunkedUploadPartRequest request) {
    if (uploadService.isPartAlreadyUploaded(request)) {
      ResponseUtils.send(ctx, new SuccessResponse("Part already uploaded"))
          .addListener(ChannelFutureListener.CLOSE);
      return;
    }

    cleanOldPartInfo(ctx);

    ChunkedUploadPartContext partInfo =
        new ChunkedUploadPartContext(
            request.sessionId(), request.userId(), request.part(), ctx.alloc().compositeBuffer());

    getPartAttribute(ctx).set(partInfo);
  }

  @RequestMapping(method = "POST", path = ApiEndpoints.FILES_CHUNKED_UPLOAD)
  public void handlePartContent(ChannelHandlerContext ctx, HttpContent content) {
    ChunkedUploadPartContext currentPart = getPartAttribute(ctx).get();

    if (currentPart == null) {
      throw new MissingUploadContextException();
    }

    addChunk(currentPart, content);

    if (content instanceof LastHttpContent) {
      finalizePartUpload(ctx, currentPart);
    }
  }

  private void complete(ChannelHandlerContext ctx, CompleteChunkedUploadRequest request) {
    UUID createdId = uploadService.completeChunkedUpload(request);
    ResponseUtils.send(ctx, new CreatedResponse(createdId, "File successfully uploaded"));
  }

  private void cleanOldPartInfo(ChannelHandlerContext ctx) {
    ChunkedUploadPartContext partInfo = getPartAttribute(ctx).get();

    if (partInfo != null && partInfo.accumulator() != null && partInfo.accumulator().refCnt() > 0) {
      partInfo.accumulator().release();
    }
  }

  private void addChunk(ChunkedUploadPartContext currentPart, HttpContent content) {
    CompositeByteBuf accumulator = currentPart.accumulator();
    ByteBuf chunk = content.content();

    if (chunk.isReadable()) {
      accumulator.addComponent(chunk.retain());
    }
  }

  private void finalizePartUpload(ChannelHandlerContext ctx, ChunkedUploadPartContext currentPart) {
    CompositeByteBuf accumulator = currentPart.accumulator();

    try (ByteBufInputStream inputStream = new ByteBufInputStream(accumulator)) {
      uploadService.uploadPart(
          new ChunkedUploadPartDto(
              currentPart.sessionId(),
              currentPart.userId(),
              currentPart.partNumber(),
              inputStream));
    } catch (IOException e) {
      throw new UploadPartIOException(ctx, e);
    } finally {
      accumulator.release();
      getPartAttribute(ctx).set(null);
    }
  }

  private Attribute<ChunkedUploadPartContext> getPartAttribute(ChannelHandlerContext ctx) {
    return ctx.channel().attr(NettyAttributes.CHUNKED_UPLOAD_PART_INFO);
  }
}
