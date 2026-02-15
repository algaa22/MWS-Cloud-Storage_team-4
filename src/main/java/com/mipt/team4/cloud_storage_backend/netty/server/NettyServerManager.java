package com.mipt.team4.cloud_storage_backend.netty.server;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.channel.MainChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NettyServerManager {
  private final AtomicBoolean stopping = new AtomicBoolean(false);
  private final CountDownLatch startupLatch;

  private final MainChannelInitializer httpChannelInitializer;
  private final MainChannelInitializer httpsChannelInitializer;
  private final NettyConfig nettyConfig;

  private Channel httpServerChannel;
  private Channel httpsServerChannel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public NettyServerManager(
      MainChannelInitializer httpChannelInitializer,
      MainChannelInitializer httpsChannelInitializer,
      NettyConfig nettyConfig) {
    this.httpChannelInitializer = httpChannelInitializer;
    this.httpsChannelInitializer = httpsChannelInitializer;
    this.nettyConfig = nettyConfig;

    int serversCount = nettyConfig.enableHttps() ? 2 : 1;
    startupLatch = new CountDownLatch(serversCount);
  }

  public void start() {
    bossGroup = createEventLoopGroup(nettyConfig.bossThreads());
    workerGroup = createEventLoopGroup(nettyConfig.workerThreads());

    try {
      startServers();
      addCloseListeners();
    } catch (Exception e) {
      stop();

      throw new ServerStartException(e);
    }
  }

  @PreDestroy
  public void stop() {
    if (!stopping.compareAndSet(false, true)) {
      return;
    }

    log.info("Stopping servers...");

    closeServers();
    shutdownThreads();
  }

  private void startServers() throws InterruptedException {
    httpServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTP);

    if (nettyConfig.enableHttps()) {
      httpsServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTPS);
    }
  }

  private void addCloseListeners() throws InterruptedException {
    Promise<Void> closePromise = bossGroup.next().newPromise();

    if (httpsServerChannel != null) {
      httpsServerChannel
          .closeFuture()
          .addListener(
              f -> {
                log.info("HTTPS server stopped");
                closePromise.trySuccess(null);
              });
    }

    httpServerChannel
        .closeFuture()
        .addListener(
            f -> {
              log.info("HTTP server stopped");
              closePromise.trySuccess(null);
            });

    log.info("Servers are running. Waiting for any channel to close...");
  }

  private void closeServers() {
    if (httpsServerChannel != null && httpsServerChannel.isOpen()) {
      httpsServerChannel.close().syncUninterruptibly();
    }

    if (httpServerChannel != null && httpServerChannel.isOpen()) {
      httpServerChannel.close().syncUninterruptibly();
    }
  }

  private void shutdownThreads() {
    int queryPeriod = nettyConfig.shutdown().queryPeriodSec();
    int timeout = nettyConfig.shutdown().timeoutSec();

    Future<?> bossShutdown = bossGroup.shutdownGracefully(queryPeriod, timeout, TimeUnit.SECONDS);

    Future<?> workerShutdown =
        workerGroup.shutdownGracefully(queryPeriod, timeout, TimeUnit.SECONDS);

    try {
      workerShutdown.await(timeout + 1, TimeUnit.SECONDS);
      bossShutdown.await(timeout + 1, TimeUnit.SECONDS);

      log.info("All threads finished shutdown");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      log.error("Shutdown interrupted", e);
    }
  }

  private Channel startServer(
      EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerProtocol protocol)
      throws InterruptedException {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childHandler(
            protocol == ServerProtocol.HTTP ? httpChannelInitializer : httpsChannelInitializer);

    int port = protocol == ServerProtocol.HTTPS ? nettyConfig.httpsPort() : nettyConfig.httpPort();

    Channel channel = bootstrap.bind(port).sync().channel();

    log.info("{} server starting on port {}...", protocol.name(), port);
    startupLatch.countDown();

    return channel;
  }

  private EventLoopGroup createEventLoopGroup(int numThreads) {
    return new MultiThreadIoEventLoopGroup(numThreads, NioIoHandler.newFactory());
  }

  public enum ServerProtocol {
    HTTP,
    HTTPS
  }
}
