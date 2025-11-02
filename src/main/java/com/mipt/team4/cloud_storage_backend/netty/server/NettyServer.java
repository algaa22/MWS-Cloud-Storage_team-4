package com.mipt.team4.cloud_storage_backend.netty.server;

import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.handler.HTTPRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
  private final HTTPRequestHandler requestHandler;
  private final NettyConfig config;

  public NettyServer(NettyConfig config, HTTPRequestHandler requestHandler) {
    this.config = config;
    this.requestHandler = requestHandler;
  }

  public void start() {
    try (EventLoopGroup bossGroup =
            new MultiThreadIoEventLoopGroup(config.getBossThreads(), NioIoHandler.newFactory());
        EventLoopGroup workerGroup =
            new MultiThreadIoEventLoopGroup(config.getWorkerThreads(), NioIoHandler.newFactory())) {

      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap
          .group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new CustomChannelInitializer());

      ChannelFuture future = bootstrap.bind(config.getPort()).sync();
      logger.info("Netty server started on port " + config.getPort());

      future.channel().closeFuture().sync();
      logger.info("Netty server stopped");

    } catch (Exception e) {
      throw new ServerStartException(e);
    }
  }

  class CustomChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
      ChannelPipeline pipeline = socketChannel.pipeline();

      pipeline.addLast(new HttpServerCodec());
      pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
      pipeline.addLast(requestHandler);
    }
  }
}
