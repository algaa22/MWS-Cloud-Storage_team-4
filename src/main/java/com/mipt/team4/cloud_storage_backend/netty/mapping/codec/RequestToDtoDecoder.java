package com.mipt.team4.cloud_storage_backend.netty.mapping.codec;

import com.mipt.team4.cloud_storage_backend.netty.mapping.DtoAssembler;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RouteRegistry;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RoutedMessage;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class RequestToDtoDecoder extends MessageToMessageDecoder<HttpRequest> {
  private final RouteRegistry routeRegistry;
  private final DtoAssembler dtoAssembler;

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpRequest request, List<Object> out) {
    String method = request.method().name();
    String path = request.uri().split("\\?")[0];

    Class<?> dtoClass = routeRegistry.getDto(method, path);

    if (dtoClass == null) {
      ReferenceCountUtil.retain(request);
      out.add(request);
      return;
    }

    Object dto = dtoAssembler.assemble(ctx, dtoClass, request);
    out.add(new RoutedMessage(dto, method, path));
  }
}
