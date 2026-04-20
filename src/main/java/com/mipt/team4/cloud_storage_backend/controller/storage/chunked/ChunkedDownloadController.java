package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.netty.input.ChunkedDownloadInput;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.utils.converter.ContentRangeConverter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChunkedDownloadController {
  private final ObjectProvider<ChunkedDownloadInput> inputProvider;
  private final StorageProps storageProps;
  private final FileService fileService;

  public void download(ChannelHandlerContext ctx, ChunkedDownloadRequest request) {
    FileDownloadInfoDto file = fileService.download(request);
    boolean isPartial = file.range() != null;

    HttpResponse header = buildDownloadHttpHeader(file, isPartial);
    ResponseUtils.write(ctx, header);

    ChunkedDownloadInput input =
        inputProvider.getObject(
            file.stream(),
            storageProps.rest().fileDownloadChunkSize(),
            getContentLength(file, isPartial));

    ResponseUtils.send(ctx, input).addListener(ChannelFutureListener.CLOSE);
  }

  private HttpResponse buildDownloadHttpHeader(FileDownloadInfoDto file, boolean isPartial) {
    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, getStatus(isPartial));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, getMimeType(file));
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, getContentLength(file, isPartial));
    response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);

    if (isPartial) {
      response
          .headers()
          .set(
              HttpHeaderNames.CONTENT_RANGE,
              ContentRangeConverter.toServerRangeString(file.range(), file.totalSize()));
    }

    return response;
  }

  private static HttpResponseStatus getStatus(boolean isPartial) {
    return isPartial ? HttpResponseStatus.PARTIAL_CONTENT : HttpResponseStatus.OK;
  }

  private static String getMimeType(FileDownloadInfoDto file) {
    return file.mimeType() != null
        ? file.mimeType()
        : HttpHeaderValues.APPLICATION_OCTET_STREAM.toString();
  }

  private static long getContentLength(FileDownloadInfoDto file, boolean isPartial) {
    return isPartial ? file.range().end() - file.range().start() + 1 : file.totalSize();
  }
}
