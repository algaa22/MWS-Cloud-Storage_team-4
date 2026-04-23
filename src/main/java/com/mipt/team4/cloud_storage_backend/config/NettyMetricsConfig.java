package com.mipt.team4.cloud_storage_backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.netty.buffer.PooledByteBufAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyMetricsConfig {
  @Bean
  public NettyAllocatorMetrics nettyAllocatorMetrics(MeterRegistry registry) {
    NettyAllocatorMetrics metrics = new NettyAllocatorMetrics(PooledByteBufAllocator.DEFAULT);
    metrics.bindTo(registry);

    return metrics;
  }
}
