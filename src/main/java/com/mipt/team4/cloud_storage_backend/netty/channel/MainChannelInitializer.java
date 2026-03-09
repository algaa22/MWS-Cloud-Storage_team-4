package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.netty.handlers.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.ObjectProvider;

public class MainChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final PipelineBuilder pipelineBuilder;
  private final SslContextFactory sslContextFactory;
  private final ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandlers;

  private final NettyConfig nettyConfig;
  private final ServerProtocol protocol;

  public MainChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandlers,
      NettyConfig nettyConfig,
      ServerProtocol protocol) {
    this.pipelineBuilder = pipelineBuilder;
    this.sslContextFactory = sslContextFactory;
    this.protocolNegotiationHandlers = protocolNegotiationHandlers;
    this.nettyConfig = nettyConfig;
    this.protocol = protocol;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) {
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
