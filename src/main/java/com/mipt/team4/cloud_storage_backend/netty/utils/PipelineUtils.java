package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.CorsHandler;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.HttpTrafficStrategySelector;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;

public class PipelineUtils {

  public static void buildHttp11Pipeline(
      ChannelPipeline pipeline,
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    pipeline.addLast(new HttpServerCodec());
    finalizeHttpPipeline(pipeline, fileController, directoryController, userController);
  }

  public static void finalizeHttpPipeline(
      ChannelPipeline pipeline,
      FileController fileController,
      DirectoryController directoryController,
      UserController userController) {
    pipeline.addLast(new CorsHandler());
    pipeline.addLast(
        new HttpTrafficStrategySelector(fileController, directoryController, userController));
  }
}
