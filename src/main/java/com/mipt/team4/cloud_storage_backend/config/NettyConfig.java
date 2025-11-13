package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public enum NettyConfig {
  INSTANCE;

  private final int port;
  private final int bossThreads;
  private final int workerThreads;

  NettyConfig() {
    ConfigSource source = new EnvironmentConfigSource(".env");

    this.port = source.getInt("netty.port").orElseThrow();
    this.bossThreads = source.getInt("netty.boss-threads").orElseThrow();
    this.workerThreads = source.getInt("netty.worker-threads").orElseThrow();
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
}
