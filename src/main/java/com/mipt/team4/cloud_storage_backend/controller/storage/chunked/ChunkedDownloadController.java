package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
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

  public void download(ChannelHandlerContext ctx, StartChunkedDownloadRequest request) {
    FileDownloadResponse file = fileService.downloadFile(request);

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.size());
    response
        .headers()
        .set(
            HttpHeaderNames.CONTENT_TYPE,
            file.mimeType() != null ? file.mimeType() : "application/octet-stream");

    ctx.write(response);

    CustomChunkedInput input =
        inputProvider.getObject(
            file.stream(), storageConfig.rest().fileDownloadChunkSize(), file.size());

    ctx.writeAndFlush(input).addListener(ChannelFutureListener.CLOSE);
  }
}
