package com.mipt.team4.cloud_storage_backend.netty.channel;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;

public class Http2StreamInitializer extends ChannelInitializer<Channel> {

  private final FileController fileController;
  private final DirectoryController directoryController;
  private final UserController userController;

  public Http2StreamInitializer(
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    this.fileController = fileController;
    this.directoryController = directoryController;
    this.userController = userController;
  }

  @Override
  protected void initChannel(Channel channel) {
    ChannelPipeline pipeline = channel.pipeline();

    pipeline.addLast(new Http2StreamFrameToHttpObjectCodec(true));
    PipelineUtils.finalizeHttpPipeline(
        pipeline, fileController, directoryController, userController);
  }
}
