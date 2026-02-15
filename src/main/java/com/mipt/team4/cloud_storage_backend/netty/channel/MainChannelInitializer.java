package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.GlobalErrorHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import org.springframework.beans.factory.ObjectProvider;

public class MainChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final PipelineBuilder pipelineBuilder;
  private final SslContextFactory sslContextFactory;
  private final ObjectProvider<GlobalErrorHandler> globalErrorHandler;
  private final ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler;

  private final NettyConfig nettyConfig;

  private final ServerProtocol protocol;

  public MainChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<GlobalErrorHandler> globalErrorHandler,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyConfig nettyConfig,
      ServerProtocol protocol) {
    this.pipelineBuilder = pipelineBuilder;
    this.sslContextFactory = sslContextFactory;
    this.globalErrorHandler = globalErrorHandler;
    this.protocolNegotiationHandler = protocolNegotiationHandler;
    this.nettyConfig = nettyConfig;
    this.protocol = protocol;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel)
      throws IOException,
          UnrecoverableKeyException,
          CertificateException,
          NoSuchAlgorithmException,
          KeyStoreException {
    ChannelPipeline pipeline = socketChannel.pipeline();

    if (nettyConfig.enableLogging()) {
      pipeline.addFirst(new LoggingHandler(LogLevel.INFO));
    }

    if (protocol == ServerProtocol.HTTPS) {
      pipeline.addLast(sslContextFactory.createFromResources().newHandler(socketChannel.alloc()));
      pipeline.addLast(protocolNegotiationHandler.getObject());
    } else {
      pipelineBuilder.buildHttp11Pipeline(pipeline);
    }

    pipeline.addLast(globalErrorHandler.getObject());
  }
}
