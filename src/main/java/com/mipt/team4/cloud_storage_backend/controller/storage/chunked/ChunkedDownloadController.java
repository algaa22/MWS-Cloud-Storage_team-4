package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChunkedDownloadController {
  private final ObjectProvider<CustomChunkedInput> inputProvider;
  private final StorageConfig storageConfig;
  private final FileService fileService;

  public void download(ChannelHandlerContext ctx, ChunkedDownloadRequest request) {
    FileDownloadInfoDto file = fileService.download(request);

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.size());
    response
        .headers()
        .set(
            HttpHeaderNames.CONTENT_TYPE,
            file.mimeType() != null ? file.mimeType() : "application/octet-stream");

    ResponseUtils.write(ctx, response);

    CustomChunkedInput input =
        inputProvider.getObject(
            file.stream(), storageConfig.rest().fileDownloadChunkSize(), file.size());

    ResponseUtils.send(ctx, input).addListener(ChannelFutureListener.CLOSE);
  }
}
