package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.YamlConfigFactory;

public enum NettyConfig {
  INSTANCE;

  private final int httpPort;
  private final int httpsPort;
  private final int bossThreads;
  private final int workerThreads;
  private final int startTimeoutSec;
  private final int shutdownTimeoutSec;
  private final int shutdownQueryPeriodSec;
  private final boolean enableHttps;
  private final boolean enableLogging;

  NettyConfig() {
    ConfigSource source = YamlConfigFactory.INSTANCE.getDefault();

    this.httpPort = source.getInt("netty.http-port").orElseThrow();
    this.httpsPort = source.getInt("netty.https-port").orElseThrow();
    this.bossThreads = source.getInt("netty.boss-threads").orElseThrow();
    this.workerThreads = source.getInt("netty.worker-threads").orElseThrow();
    this.startTimeoutSec = source.getInt("netty.start.timeout-sec").orElseThrow();
    this.shutdownTimeoutSec = source.getInt("netty.shutdown.timeout-sec").orElseThrow();
    this.shutdownQueryPeriodSec = source.getInt("netty.shutdown.quiet-period-sec").orElseThrow();
    this.enableHttps = source.getBoolean("netty.enable-https").orElseThrow();
    this.enableLogging = source.getBoolean("netty.enable-logging").orElseThrow();
  }

  public int getHttpPort() {
    return httpPort;
  }

  public int getHttpsPort() {
    return httpsPort;
  }

  public int getBossThreads() {
    return bossThreads;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public int getStartTimeoutSec() {
    return startTimeoutSec;
  }

  public int getShutdownTimeoutSec() {
    return shutdownTimeoutSec;
  }

  public int getShutdownQueryPeriodSec() {
    return shutdownQueryPeriodSec;
  }

  public boolean isEnableHttps() {
    return enableHttps;
  }

  public boolean isEnableLogging() {
    return enableLogging;
  }
}
