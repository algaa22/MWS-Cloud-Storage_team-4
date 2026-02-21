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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedInput;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
    String name = RequestUtils.getRequiredQueryParam(request, "name");
    UUID parentId = RequestUtils.getOptionalUuidQueryParam(request, "parentId");

    FileDownloadDto fileDownload =
        fileController.downloadFile(new SimpleFileOperationRequest(parentId, name, userToken));

    HttpResponse response =
        new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);

    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileDownload.size());
    String contentType =
        fileDownload.mimeType() != null ? fileDownload.mimeType() : "application/octet-stream";
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);

    String encodedFileName =
        URLEncoder.encode(fileDownload.name(), StandardCharsets.UTF_8).replace("+", "%20");

    response
        .headers()
        .set(
            HttpHeaderNames.CONTENT_DISPOSITION,
            "attachment; filename=\""
                + fileDownload.name()
                + "\"; filename*=UTF-8''"
                + encodedFileName);

    response.headers().set("X-File-Name", fileDownload.name());
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
