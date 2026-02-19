package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedInput;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ChunkedDownloadHandler {
  private final FileController fileController;
  private final StorageConfig storageConfig;

  // TODO: refactor
  public void startChunkedDownload(ChannelHandlerContext ctx, HttpRequest request)
      throws UserNotFoundException,
      StorageFileNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException,
          HeaderNotFoundException {
    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    String filePath = RequestUtils.getRequiredQueryParam(request, "path");

    FileDownloadDto fileDownload =
        fileController.downloadFile(new SimpleFileOperationRequest(filePath, userToken));

    HttpResponse response =
        new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileDownload.size());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
    response.headers().set("X-File-Path", fileDownload.path());
    response.headers().set("X-File-Size", fileDownload.size());
    ctx.write(response);

    ChunkedInput<HttpContent> chunkedInput =
        new CustomChunkedInput(
            fileDownload.stream(),
            storageConfig.rest().fileDownloadChunkSize(),
            fileDownload.size());

    ChannelFuture future = ctx.writeAndFlush(chunkedInput);
    future.addListener(ChannelFutureListener.CLOSE);
  }
}
