package com.mipt.team4.cloud_storage_backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.controller.Controller;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserCreateDto;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UserController extends Controller {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @Override
  public FullHttpResponse handleRequest(FullHttpRequest request) {
    // TODO: обработка HTTP запроса, возврат HTTP ответа

    // // POST /user/  (создать пользователя)
    //    if (req.method() == HttpMethod.POST && req.uri().equals("/user/")) {
    //      try (ByteBufInputStream in = new ByteBufInputStream(req.content())) {
    //        UserDto dto = mapper.readValue((InputStream) in, UserDto.class);
    //        UserDto created = service.createUser(dto);
    //        sendJson(ctx, created);
    //      }
    //      return;
    //    }
    //    // GET /user/{id}
    //    if (req.method() == HttpMethod.GET && req.uri().startsWith("/user/")) {
    //      String idStr = req.uri().substring("/user/".length());
    //      UUID id = UUID.fromString(idStr);
    //      UserDto dto = service.getUser(id);
    //      sendJson(ctx, dto);
    //      return;
    //    }
    //
    //    // DELETE /user/{id}
    //    if (req.method() == HttpMethod.DELETE && req.uri().startsWith("/user/")) {
    //      String idStr = req.uri().substring("/user/".length());
    //      UUID id = UUID.fromString(idStr);
    //      service.deleteUser(id);
    //      sendResponse(ctx, HttpResponseStatus.NO_CONTENT, "User deleted");
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
    // TODO: переделать (я сам)
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
    // TODO: переделать (я сам)
    FullHttpResponse res = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        ctx.alloc().buffer().writeBytes(message.getBytes(StandardCharsets.UTF_8))
    );
    ctx.writeAndFlush(res);
  }
}
