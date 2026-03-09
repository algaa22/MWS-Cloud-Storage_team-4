package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
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
    public void start(ChannelHandlerContext ctx, HttpRequest request) {
        String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
        String fileId = RequestUtils.getRequiredQueryParam(request, "id");

        FileDownloadResponse fileDownload =
                fileController.downloadFile(new SimpleFileOperationRequest(fileId, userToken));

        HttpResponse response =
                new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);

        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileDownload.size());
        String contentType =
                fileDownload.mimeType() != null ? fileDownload.mimeType() : "application/octet-stream";
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set("X-File-Size", fileDownload.size());

        ResponseUtils.write(ctx, response);

        ChunkedInput<HttpContent> chunkedInput =
                new CustomChunkedInput(
                        fileDownload.stream(),
                        storageConfig.rest().fileDownloadChunkSize(),
                        fileDownload.size());

        ResponseUtils.send(ctx, chunkedInput).addListener(ChannelFutureListener.CLOSE);
    }
}
