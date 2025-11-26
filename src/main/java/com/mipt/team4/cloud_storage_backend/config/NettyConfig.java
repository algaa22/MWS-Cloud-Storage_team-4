package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;

public enum NettyConfig {
  INSTANCE;

  private final int port;
  private final int bossThreads;
  private final int workerThreads;
  private final int startTimeoutSec;

  NettyConfig() {
    ConfigSource source = new YamlConfigSource("config.yml");

    this.port = source.getInt("netty.port").orElseThrow();
    this.bossThreads = source.getInt("netty.boss-threads").orElseThrow();
    this.workerThreads = source.getInt("netty.worker-threads").orElseThrow();
    this.startTimeoutSec = source.getInt("netty.start-timeout-sec").orElseThrow();
  }

  public int getPort() {
    return port;
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
}
