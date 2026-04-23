package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty")
public record NettyProps(
    boolean enableLogging,
    boolean enableHttps,
    boolean httpsRedirect,
    int httpPort,
    int httpsPort,
    int bossThreads,
    int workerThreads,
    int idleTimeoutSec,
    Start start,
    Shutdown shutdown) {

  public record Start(int timeoutSec) {}

  public record Shutdown(int timeoutSec, int queryPeriodSec) {}
}
