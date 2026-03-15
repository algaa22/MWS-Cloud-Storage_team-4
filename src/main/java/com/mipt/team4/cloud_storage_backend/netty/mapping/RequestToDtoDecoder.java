package com.mipt.team4.cloud_storage_backend.netty.mapping;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@Sharable
public class RequestToDtoDecoder extends MessageToMessageDecoder<FullHttpRequest> {

  @Override
  protected void decode(
      ChannelHandlerContext channelHandlerContext, FullHttpRequest request, List<Object> list)
      throws Exception {}
}
