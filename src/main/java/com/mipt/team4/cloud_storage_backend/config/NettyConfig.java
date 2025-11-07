package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;

public class NettyConfig {
  private final int port;
  private final int bossThreads;
  private final int workerThreads;

  public NettyConfig(int port, int bossThreads, int workerThreads) {
    this.port = port;
    this.bossThreads = bossThreads;
    this.workerThreads = workerThreads;
  }

  public static NettyConfig from(ConfigSource source) {
    return new NettyConfig(
        source.getInt("netty.port").orElseThrow(),
        source.getInt("netty.boss-threads").orElseThrow(),
        source.getInt("netty.worker-threads").orElseThrow());
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
