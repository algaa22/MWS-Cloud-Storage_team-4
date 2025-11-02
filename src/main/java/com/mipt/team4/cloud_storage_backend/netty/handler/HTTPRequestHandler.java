package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.router.RequestRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;

public class HTTPRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final RequestRouter router;

  public HTTPRequestHandler(FileController fileController, UserController userController) {
    router = new RequestRouter(fileController, userController);
  }

  @Override
  protected void channelRead0(
      ChannelHandlerContext ctx, FullHttpRequest request) {
    FullHttpResponse response = router.route(request);
    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // TODO: написать нормальный обработчик
    cause.printStackTrace();
    ctx.close();
  }
}
