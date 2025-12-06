package com.mipt.team4.cloud_storage_backend.netty.server;

import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.cors.CompleteCorsHandler;
import com.mipt.team4.cloud_storage_backend.netty.pipeline.PipelineSelector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
  private final CountDownLatch startupLatch = new CountDownLatch(1);
  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  private Channel serverChannel;

  public NettyServer(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;
  }

  public void start() {
    try (EventLoopGroup bossGroup =
            new MultiThreadIoEventLoopGroup(
                NettyConfig.INSTANCE.getBossThreads(), NioIoHandler.newFactory());
        EventLoopGroup workerGroup =
            new MultiThreadIoEventLoopGroup(
                NettyConfig.INSTANCE.getWorkerThreads(), NioIoHandler.newFactory())) {

      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap
          .group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .option(ChannelOption.SO_REUSEADDR, true)
          .childHandler(new CustomChannelInitializer());

      ChannelFuture future = bootstrap.bind(NettyConfig.INSTANCE.getPort()).sync();
      logger.info("Netty server started on port " + NettyConfig.INSTANCE.getPort());

      startupLatch.countDown();

      serverChannel = future.channel();
      serverChannel.closeFuture().sync();

      logger.info("Netty server stopped");
    } catch (Exception e) {
      throw new ServerStartException(e);
    }
  }

  public void stop() {
    if (serverChannel != null) serverChannel.close();
  }

  public CountDownLatch getStartupLatch() {
    return startupLatch;
  }

  class CustomChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel)  {
      ChannelPipeline pipeline = socketChannel.pipeline();

      pipeline.addLast("httpCodec", new HttpServerCodec());
      pipeline.addLast("cors", new CompleteCorsHandler());
      pipeline.addLast("pipeSelector", new PipelineSelector(fileController, directoryController, userController));
    }
  }
}
