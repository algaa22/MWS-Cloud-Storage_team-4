package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.UserGetDto;
import java.io.InputStream;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import service.UserService;

public class UserController extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    // POST /user/  (создать пользователя)
    if (req.method() == HttpMethod.POST && req.uri().equals("/user/")) {
      try (ByteBufInputStream in = new ByteBufInputStream(req.content())) {
        UserGetDto dto = mapper.readValue((InputStream) in, UserGetDto.class);
        UserGetDto created = service.createUser(dto);
        sendJson(ctx, created);
      }
      return;
    }
    // GET /user/{id}
    if (req.method() == HttpMethod.GET && req.uri().startsWith("/user/")) {
      String idStr = req.uri().substring("/user/".length());
      UUID id = UUID.fromString(idStr);
      UserGetDto dto = service.getUser(id);
      sendJson(ctx, dto);
      return;
    }

    // DELETE /user/{id}
    if (req.method() == HttpMethod.DELETE && req.uri().startsWith("/user/")) {
      String idStr = req.uri().substring("/user/".length());
      UUID id = UUID.fromString(idStr);
      service.deleteUser(id);
      sendResponse(ctx, HttpResponseStatus.NO_CONTENT, "User deleted");
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
