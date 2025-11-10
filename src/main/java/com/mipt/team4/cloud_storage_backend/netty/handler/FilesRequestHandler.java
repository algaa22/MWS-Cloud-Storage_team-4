package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.List;

public class FilesRequestHandler {
  private final FileController fileController;

  public FilesRequestHandler(FileController fileController) {
    this.fileController = fileController;
  }

  public void handleGetFilePathsListRequest(
      ChannelHandlerContext ctx, String userId) {
    List<String> paths = fileController.getFilePathsList(userId);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    ArrayNode filesArray = mapper.createArrayNode();

    if (paths != null) {
      for (String path : paths) {
        ObjectNode fileNode = mapper.createObjectNode();
        fileNode.put("path", path);
        filesArray.add(fileNode);
      }
    }

    rootNode.set("files", filesArray);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleGetFileInfoRequest(
      ChannelHandlerContext ctx, String fileId, String userId) {
    FileDto fileDto = fileController.getFileInfo(fileId, userId);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("ID", fileDto.fileId());
    rootNode.put("OwnerID", fileDto.ownerId());
    rootNode.put("Path", fileDto.path());
    rootNode.put("Type", fileDto.type());
    rootNode.put("Visibility", fileDto.visibility());
    rootNode.put("Size", fileDto.size());
    rootNode.put("IsDeleted", fileDto.isDeleted());
    rootNode.put("Tags", FileTagsMapper.toString(fileDto.tags()));

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleDeleteFileRequest(
      ChannelHandlerContext ctx, String fileId, String userId) {}

  public void handleChangeFileMetadataRequest(
      ChannelHandlerContext ctx, String fileId, String userId) {}

  public void handleGetFileRequest(
      ChannelHandlerContext ctx, String fileId, String userId) {}
}
