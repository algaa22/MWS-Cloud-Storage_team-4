package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FileController extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final ObjectMapper mapper = new ObjectMapper();
  private final FileService service;

  public FileController(FileService service) {
    this.service = service;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    // POST /file/

    if (req.method() == HttpMethod.POST && req.uri().equals("/file/upload")) {
      String fileName = req.headers().get("X-File-Name");
      String contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
      try (ByteBufInputStream fileStream = new ByteBufInputStream(req.content())) {
        FileDto created = service.uploadFile(fileName, contentType, fileStream);
        sendJson(ctx, created);
      }
      return;
    }
    // GET /file/{id}
    if (req.method() == HttpMethod.GET && req.uri().startsWith("/file/")) {
      String idStr = req.uri().substring("/file/".length());
      UUID id = UUID.fromString(idStr);
      FileDto dto = service.getFileInfo(String.valueOf(id));
      sendJson(ctx, dto);
      return;
    }
    // DELETE /file/{id}
    if (req.method() == HttpMethod.DELETE && req.uri().startsWith("/file/")) {
      String idStr = req.uri().substring("/file/".length());
      UUID id = UUID.fromString(idStr);
      service.deleteFile(String.valueOf(id));
      sendResponse(ctx, HttpResponseStatus.NO_CONTENT, "File deleted");
      return;
    }

    sendResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not found");
  }

  private void sendJson(ChannelHandlerContext ctx, Object obj) throws Exception {
    String json = mapper.writeValueAsString(obj);
    FullHttpResponse res = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        ctx.alloc().buffer().writeBytes(json.getBytes(StandardCharsets.UTF_8))
    );
    res.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    ctx.writeAndFlush(res);
  }

  private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
    FullHttpResponse res = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        ctx.alloc().buffer().writeBytes(message.getBytes(StandardCharsets.UTF_8))
    );
    ctx.writeAndFlush(res);
  }
}


