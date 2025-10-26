package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.FileDto;
import java.io.InputStream;
import service.FileService;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

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
    // POST /file/  (создать файл/метаданные)
    if (req.method() == HttpMethod.POST && req.uri().equals("/file/")) {
      try (ByteBufInputStream in = new ByteBufInputStream(req.content())) {
        FileDto dto = mapper.readValue((InputStream) in, FileDto.class);
        FileDto created = service.createFile(dto);
        sendJson(ctx, created);
      }
      return;
    }
    // GET /file/{id}
    if (req.method() == HttpMethod.GET && req.uri().startsWith("/file/")) {
      String idStr = req.uri().substring("/file/".length());
      UUID id = UUID.fromString(idStr);
      FileDto dto = service.getFile(id);
      sendJson(ctx, dto);
      return;
    }
    // PUT /file/{id}
    if (req.method() == HttpMethod.PUT && req.uri().startsWith("/file/")) {
      String idStr = req.uri().substring("/file/".length());
      UUID id = UUID.fromString(idStr);
      try (ByteBufInputStream in = new ByteBufInputStream(req.content())) {
        FileDto dto = mapper.readValue((InputStream) in, FileDto.class);
        FileDto updated = service.updateFile(id, dto);
        sendJson(ctx, updated);
      }
      return;
    }
    // DELETE /file/{id}
    if (req.method() == HttpMethod.DELETE && req.uri().startsWith("/file/")) {
      String idStr = req.uri().substring("/file/".length());
      UUID id = UUID.fromString(idStr);
      service.deleteFile(id);
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

