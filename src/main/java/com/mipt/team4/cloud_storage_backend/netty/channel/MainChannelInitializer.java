package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.config.props.NettyProps;
import com.mipt.team4.cloud_storage_backend.netty.constants.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.http.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

@Sharable
@RequiredArgsConstructor
public class MainChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandlers;
  private final PipelineBuilder pipelineBuilder;
  private final SslContextFactory sslContextFactory;

  private final NettyProps nettyConfig;
  private final ServerProtocol protocol;
  private final ChannelGroup allChannels;

  @Override
  protected void initChannel(SocketChannel socketChannel) {
    allChannels.add(socketChannel);

    ChannelPipeline pipeline = socketChannel.pipeline();

    if (nettyConfig.enableLogging()) {
      pipeline.addFirst(PipelineHandlerNames.LOGGING, new LoggingHandler(LogLevel.INFO));
    }

    pipeline.addLast(
        PipelineHandlerNames.IDLE_STATE, new IdleStateHandler(0, 0, nettyConfig.idleTimeoutSec()));

    if (protocol == ServerProtocol.HTTPS) {
      pipeline.addLast(
          PipelineHandlerNames.SSL,
          sslContextFactory.createFromResources().newHandler(socketChannel.alloc()));
      pipeline.addLast(
          PipelineHandlerNames.PROTOCOL_NEGOTIATION, protocolNegotiationHandlers.getObject());
    } else {
      pipelineBuilder.buildHttp11Pipeline(pipeline);
    }
  }
}
