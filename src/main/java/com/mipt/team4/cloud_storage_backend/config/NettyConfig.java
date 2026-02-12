package com.mipt.team4.cloud_storage_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty")
public record NettyConfig(
    boolean enableLogging,
    boolean enableHttps,
    int httpPort,
    int httpsPort,
    int bossThreads,
    int workerThreads,
    Start start,
    Shutdown shutdown) {

  public record Start(int timeoutSec) {}

  public record Shutdown(int timeoutSec, int queryPeriodSec) {}
}
