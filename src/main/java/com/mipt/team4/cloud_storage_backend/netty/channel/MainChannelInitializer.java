package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.CorsSecurityHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.HttpTrafficStrategySelector.PipelineType;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.HttpTrafficStrategySelector;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class MainChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;
  private final ServerProtocol protocol;

  public MainChannelInitializer(FileController fileController,
      DirectoryController directoryController, UserController userController,
      ServerProtocol protocol) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;
    this.protocol = protocol;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel)
      throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
    ChannelPipeline pipeline = socketChannel.pipeline();

    if (protocol == ServerProtocol.HTTPS) {
      pipeline.addLast(SslContextFactory.createFromResources().newHandler(socketChannel.alloc()));
      pipeline.addLast(
          new ProtocolNegotiationHandler(fileController, directoryController, userController));
    } else {
      PipelineUtils.buildHttp11Pipeline(pipeline, fileController, directoryController, userController);
    }
  }
}
