package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.controller.Controller;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.nio.charset.StandardCharsets;

public class FileController extends Controller {
  private final ObjectMapper mapper = new ObjectMapper();
  private final FileService service;

  public FileController(FileService service) {
    this.service = service;
  }

  @Override
  public FullHttpResponse handleRequest(FullHttpRequest request) {
    // TODO: обработка HTTP запроса, возврат HTTP ответа

    // if (req.method() == HttpMethod.POST && req.uri().equals("/file/")) {
    //      try (ByteBufInputStream in = new ByteBufInputStream(req.content())) {
    //        // Передавать FileUploadDto, НЕ FileGetDto!
    //        FileUploadDto uploadDto = mapper.readValue((InputStream) in, FileUploadDto.class);
    //        FileDto created = service.uploadFile(uploadDto);
    //        sendJson(ctx, created);
    //      }
    //      return;
    //    }
    //    // GET /file/{id}
    //    if (req.method() == HttpMethod.GET && req.uri().startsWith("/file/")) {
    //      String idStr = req.uri().substring("/file/".length());
    //      UUID id = UUID.fromString(idStr);
    //      FileDto dto = service.getFileInfo(String.valueOf(id)); // Корректный вызов: getFileInfo,
    // а не uploadFile
    //      sendJson(ctx, dto);
    //      return;
    //    }
    //    // DELETE /file/{id}
    //    if (req.method() == HttpMethod.DELETE && req.uri().startsWith("/file/")) {
    //      String idStr = req.uri().substring("/file/".length());
    //      UUID id = UUID.fromString(idStr);
    //      service.deleteFile(String.valueOf(id));
    //      sendResponse(ctx, HttpResponseStatus.NO_CONTENT, "File deleted");
    //      return;
    //    }
    //
    //    sendResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not found");

    return null;
  }

  @Override
  protected FullHttpResponse handleGet(FullHttpRequest request, String url) {
    return null;
  }

  @Override
  protected FullHttpResponse handlePost(FullHttpRequest request, String url) {
    return null;
  }

  @Override
  protected FullHttpResponse handleUpdate(FullHttpRequest request, String url) {
    return null;
  }

  private void sendJson(ChannelHandlerContext ctx, Object obj) throws Exception {
    // TODO: переделать (я сам), см. ResponseHelper
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
    // TODO: переделать (я сам), см. ResponseHelper
    FullHttpResponse res = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        ctx.alloc().buffer().writeBytes(message.getBytes(StandardCharsets.UTF_8))
    );
    ctx.writeAndFlush(res);
  }
}


