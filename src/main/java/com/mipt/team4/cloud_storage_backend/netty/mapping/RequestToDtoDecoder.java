package com.mipt.team4.cloud_storage_backend.netty.mapping;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class RequestToDtoDecoder extends MessageToMessageDecoder<FullHttpRequest> {
  private final RouteRegistry routeRegistry;
  private final DtoAssembler dtoAssembler;

  @Override
  protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) {
    String method = request.method().name();
    String path = request.uri().split("\\?")[0];

    Class<?> dtoClass = routeRegistry.getDto(method, path);

    if (dtoClass == null) {
      out.add(request.retain());
      return;
    }

    Object dto = dtoAssembler.assemble(ctx, dtoClass, request);
    out.add(dto);
  }
}
