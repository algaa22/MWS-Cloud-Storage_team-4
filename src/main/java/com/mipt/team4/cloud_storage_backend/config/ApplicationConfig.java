package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.netty.channel.MainChannelInitializer;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.GlobalErrorHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import java.security.SecureRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }

  @Bean
  public MainChannelInitializer httpChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<GlobalErrorHandler> globalErrorHandler,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyConfig nettyConfig) {
    return new MainChannelInitializer(
        pipelineBuilder,
        sslContextFactory,
        globalErrorHandler,
        protocolNegotiationHandler,
        nettyConfig,
        ServerProtocol.HTTP);
  }

  @Bean
  public MainChannelInitializer httpsChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<GlobalErrorHandler> globalErrorHandler,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyConfig nettyConfig) {
    return new MainChannelInitializer(
        pipelineBuilder,
        sslContextFactory,
        globalErrorHandler,
        protocolNegotiationHandler,
        nettyConfig,
        ServerProtocol.HTTPS);
  }
}
