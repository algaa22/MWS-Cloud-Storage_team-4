package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public class NettyConfig {
  private static volatile NettyConfig instance;
  private final int port;
  private final int bossThreads;
  private final int workerThreads;

  private NettyConfig(int port, int bossThreads, int workerThreads) {
    this.port = port;
    this.bossThreads = bossThreads;
    this.workerThreads = workerThreads;
  }

  public static NettyConfig getInstance() {
    if (instance == null) {
      synchronized (DatabaseConfig.class) {
        if (instance == null) {
          ConfigSource source = new EnvironmentConfigSource();

          instance = new NettyConfig(
                  source.getInt("netty.port").orElseThrow(),
                  source.getInt("netty.boss-threads").orElseThrow(),
                  source.getInt("netty.worker-threads").orElseThrow());
        }
      }
    }

    return instance;
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
