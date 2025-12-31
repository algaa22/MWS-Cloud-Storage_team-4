package com.mipt.team4.cloud_storage_backend.netty.server;

import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.filters.CorsFilter;
import com.mipt.team4.cloud_storage_backend.netty.filters.Http2RequestFilter;
import com.mipt.team4.cloud_storage_backend.netty.pipeline.PipelineSelector;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
  private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
  private final CountDownLatch startupLatch;
  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  private Channel httpServerChannel;
  private Channel httpsServerChannel;

  protected enum ServerProtocol {
    HTTP,
    HTTPS
  }

  public NettyServer(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;

    int serversCount = NettyConfig.INSTANCE.isEnableHttps() ? 2 : 1;
    startupLatch = new CountDownLatch(serversCount);
  }

  public void start() {
    try (EventLoopGroup bossGroup = new NioEventLoopGroup(NettyConfig.INSTANCE.getBossThreads());
        EventLoopGroup workerGroup =
            new NioEventLoopGroup(NettyConfig.INSTANCE.getWorkerThreads())) {

      // TODO: refactor
      // TODO: зачем и HTTP, и HTTPS
      httpServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTP);

      if (NettyConfig.INSTANCE.isEnableHttps())
        httpsServerChannel = startServer(bossGroup, workerGroup, ServerProtocol.HTTPS);

      if (httpsServerChannel != null) {
        httpsServerChannel.closeFuture().sync();
        logger.info("Netty HTTPS server stopped");
      }

      if (httpServerChannel != null) {
        // TODO: нормальный shutdown
        httpServerChannel.closeFuture().sync();
        logger.info("Netty HTTP server stopped");
      }
    } catch (Exception e) {
      throw new ServerStartException(e);
    }
  }

  public void stop() {
    if (httpServerChannel != null) httpServerChannel.close();
  }

  public CountDownLatch getStartupLatch() {
    return startupLatch;
  }

  private Channel startServer(
      EventLoopGroup bossGroup, EventLoopGroup workerGroup, ServerProtocol protocol)
      throws InterruptedException {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childHandler(new CustomChannelInitializer(protocol));

    int port =
        protocol == ServerProtocol.HTTPS
            ? NettyConfig.INSTANCE.getHttpsPort()
            : NettyConfig.INSTANCE.getHttpPort();

    Channel channel = bootstrap.bind(port).sync().channel();

    logger.info("Netty " + protocol.name() + " started on port " + port);
    startupLatch.countDown();

    return channel;
  }

  // TODO: в отдельный класс?
  class CustomChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerProtocol protocol;

    public CustomChannelInitializer(ServerProtocol protocol) {
      this.protocol = protocol;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel)
        throws IOException,
            UnrecoverableKeyException,
            CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException {
      ChannelPipeline pipeline = socketChannel.pipeline();

      if (protocol == ServerProtocol.HTTPS) {
        pipeline.addLast(SslContextFactory.createFromResources().newHandler(socketChannel.alloc()));
        pipeline.addLast(
            new Http2RequestFilter(fileController, directoryController, userController));
      } else {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new CorsFilter()); // TODO: нужен ли CORS в HTTP?
        pipeline.addLast(new PipelineSelector(fileController, directoryController, userController));
      }
    }
  }
}
