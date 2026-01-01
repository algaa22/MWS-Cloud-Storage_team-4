package com.mipt.team4.cloud_storage_backend.netty.server;

import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerManager {

  private static final Logger logger = LoggerFactory.getLogger(NettyServerManager.class);

  private final AtomicBoolean stopping = new AtomicBoolean(false);
  private final CountDownLatch startupLatch;
  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  private Channel httpServerChannel;
  private Channel httpsServerChannel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public NettyServerManager(FileController fileController, DirectoryController directoryController,
      UserController userController) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;

    int serversCount = NettyConfig.INSTANCE.isEnableHttps() ? 2 : 1;
    startupLatch = new CountDownLatch(serversCount);
  }

  public void start() {
    bossGroup = createEventLoopGroup(
        NettyConfig.INSTANCE.getBossThreads());

    workerGroup = createEventLoopGroup(
        NettyConfig.INSTANCE.getWorkerThreads());

    try {
      Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
      startServers();
      addCloseListeners();
    } catch (Exception e) {
      stop();

      throw new ServerStartException(e);
    }
  }

  public void stop() {
    if (!stopping.compareAndSet(false, true)) {
      return;
    }

    logger.info("Stopping server...");

    closeServers();
    shutdownThreads();
  }

  public CountDownLatch getStartupLatch() {
    return startupLatch;
  }

  private void startServers() throws InterruptedException {
    httpServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTP);

    if (NettyConfig.INSTANCE.isEnableHttps()) {
      httpsServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTPS);
    }
  }

  private void addCloseListeners() throws InterruptedException {
    Promise<Void> closePromise = bossGroup.next().newPromise();

    if (httpsServerChannel != null) {
      httpsServerChannel.closeFuture().addListener(f -> {
        logger.info("HTTPS server stopped");
        closePromise.trySuccess(null);
      });
    }

    httpServerChannel.closeFuture().addListener(f -> {
      logger.info("HTTP server stopped");
      closePromise.trySuccess(null);
    });

    logger.info("Servers are running. Waiting for any channel to close...");
    closePromise.sync();

    stop();
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
    Future<?> bossShutdown = bossGroup.shutdownGracefully(
        NettyConfig.INSTANCE.getShutdownQueryPeriodSec(),
        NettyConfig.INSTANCE.getShutdownTimeoutSec(), TimeUnit.SECONDS);

    Future<?> workerShutdown = workerGroup.shutdownGracefully(
        NettyConfig.INSTANCE.getShutdownQueryPeriodSec(),
        NettyConfig.INSTANCE.getShutdownTimeoutSec(), TimeUnit.SECONDS);

    try {
      workerShutdown.await(NettyConfig.INSTANCE.getShutdownTimeoutSec() + 1, TimeUnit.SECONDS);
      bossShutdown.await(NettyConfig.INSTANCE.getShutdownTimeoutSec() + 1, TimeUnit.SECONDS);

      logger.info("All threads finished shutdown");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      logger.error("Shutdown interrupted", e);
    }
  }

  private Channel startServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
      ServerProtocol protocol) throws InterruptedException {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childHandler(
            new MainChannelInitializer(fileController, directoryController, userController,
                protocol));

    int port = protocol == ServerProtocol.HTTPS ? NettyConfig.INSTANCE.getHttpsPort()
        : NettyConfig.INSTANCE.getHttpPort();

    Channel channel = bootstrap.bind(port).sync().channel();

    logger.info("{} server starting on port {}...", protocol.name(), port);
    startupLatch.countDown();

    return channel;
  }

  private EventLoopGroup createEventLoopGroup(int numThreads) {
    return new MultiThreadIoEventLoopGroup(numThreads, NioIoHandler.newFactory());
  }

  public enum ServerProtocol {
    HTTP, HTTPS
  }
}
