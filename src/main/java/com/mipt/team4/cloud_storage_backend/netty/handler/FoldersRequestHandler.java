package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import io.netty.channel.ChannelHandlerContext;

public record FoldersRequestHandler(FileController fileController) {
  public void handleCreateFolderRequest(ChannelHandlerContext ctx, String userToken) {}

  public void handleMoveFolderRequest(
      ChannelHandlerContext ctx, String folderId, String userToken) {}

  public void handleGetFolderContentRequest(
      ChannelHandlerContext ctx, String folderId, String userToken) {}

  public void handleRenameFolderRequest(
      ChannelHandlerContext ctx, String folderId, String userToken) {}

  public void handleDeleteFolderRequest(
      ChannelHandlerContext ctx, String folderId, String userToken) {}
}
