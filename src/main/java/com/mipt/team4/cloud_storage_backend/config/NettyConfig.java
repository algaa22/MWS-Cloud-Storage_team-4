package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.props.NettyProps;
import com.mipt.team4.cloud_storage_backend.netty.channel.MainChannelInitializer;
import com.mipt.team4.cloud_storage_backend.netty.constants.NettyMetrics;
import com.mipt.team4.cloud_storage_backend.netty.handlers.http.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {
  @Bean
  public NettyAllocatorMetrics nettyAllocatorMetrics(MeterRegistry registry) {
    NettyAllocatorMetrics metrics = new NettyAllocatorMetrics(PooledByteBufAllocator.DEFAULT);
    metrics.bindTo(registry);

    return metrics;
  }

  @Bean
  public MainChannelInitializer httpChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyProps nettyConfig,
      ChannelGroup allChannels) {
    return new MainChannelInitializer(
        protocolNegotiationHandler,
        pipelineBuilder,
        sslContextFactory,
        nettyConfig,
        ServerProtocol.HTTP,
        allChannels);
  }

  @Bean
  public MainChannelInitializer httpsChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyProps nettyConfig,
      ChannelGroup allChannels) {
    return new MainChannelInitializer(
        protocolNegotiationHandler,
        pipelineBuilder,
        sslContextFactory,
        nettyConfig,
        ServerProtocol.HTTPS,
        allChannels);
  }

  @Bean
  public ChannelGroup allChannels(MeterRegistry meterRegistry) {
    ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    Gauge.builder(NettyMetrics.NETTY_CONNECTIONS_ACTIVE, group, ChannelGroup::size)
        .description("Number of currently open TCP connections")
        .register(meterRegistry);

    return group;
  }
}
