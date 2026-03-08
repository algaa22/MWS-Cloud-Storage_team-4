package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class FilesRequestHandler {
  private final FileController fileController;
  private final ObjectMapper mapper = new ObjectMapper();

  public void handleGetFileListRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    boolean includeDirectories =
        SafeParser.parseBoolean(
            "Include directories",
            RequestUtils.getQueryParam(request, "includeDirectories", "false"));
    boolean recursive =
        SafeParser.parseBoolean(
            "Recursive", RequestUtils.getQueryParam(request, "recursive", "false"));
    Optional<String> parentId = RequestUtils.getQueryParam(request, "parentId");

    List<StorageEntity> files =
        fileController.getFileList(
            new GetFileListRequest(userToken, includeDirectories, recursive, parentId));

    ObjectNode rootNode = mapper.createObjectNode();
    ArrayNode filesArray = mapper.createArrayNode();

    if (files != null) {
      for (StorageEntity file :
          files) { // TODO: entity в контроллере? put по dto через функции jackson
        ObjectNode fileNode = mapper.createObjectNode();
        fileNode.put("id", file.getId().toString());
        fileNode.put("parentId", String.valueOf(file.getParentId()));
        fileNode.put("name", file.getName());
        fileNode.put("size", file.getSize());
        fileNode.put("tags", FileTagsMapper.toString(file.getTags()));
        fileNode.put("mimeType", file.getMimeType());
        fileNode.put("visibility", file.getVisibility());
        fileNode.put("updatedAt", file.getUpdatedAt().toString());
        fileNode.put("isDirectory", file.isDirectory());
        filesArray.add(fileNode);
      }
    }

    rootNode.set("files", filesArray);

    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleGetFileInfoRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String fileId = RequestUtils.getRequiredQueryParam(request, "id");
    StorageDto storageDto =
        fileController.getFileInfo(new SimpleFileOperationRequest(fileId, userToken, false));

    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Id", storageDto.storageId().toString());
    rootNode.put("Name", storageDto.name());
    rootNode.put(
        "ParentId", storageDto.parentId() != null ? storageDto.parentId().toString() : null);
    rootNode.put("Type", storageDto.type());
    rootNode.put("Visibility", storageDto.visibility());
    rootNode.put("Size", storageDto.size());
    rootNode.put("IsDeleted", storageDto.isDeleted());
    rootNode.put("Tags", FileTagsMapper.toString(storageDto.tags()));

    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, rootNode);
  }

    public void handleRestoreFileRequest(
            ChannelHandlerContext ctx, HttpRequest request, String userToken) {
        String fileId = RequestUtils.getRequiredQueryParam(request, "id");

        SimpleFileOperationRequest restoreRequest = new SimpleFileOperationRequest(fileId, userToken, false);

        fileController.restoreFile(restoreRequest);

        ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "File successfully restored");
    }

  public void handleDeleteFileRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken) {
    String fileId = RequestUtils.getRequiredQueryParam(request, "id");

    boolean permanent =
        SafeParser.parseBoolean(
            "Permanent delete", RequestUtils.getQueryParam(request, "permanent", "false"));

    fileController.deleteFile(new SimpleFileOperationRequest(fileId, userToken, permanent));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "File successfully deleted");
  }

  public void handleChangeFileMetadataRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String userToken) {
    String fileId = RequestUtils.getRequiredQueryParam(request, "id");

    Optional<String> newName = RequestUtils.getQueryParam(request, "newName");
    Optional<String> newParentId = RequestUtils.getQueryParam(request, "newParentId");

    Optional<String> fileVisibility =
        Optional.ofNullable(RequestUtils.getHeader(request, "X-File-New-Visibility", null));

    Optional<List<String>> fileTags =
        Optional.ofNullable(
            FileTagsMapper.toList(RequestUtils.getHeader(request, "X-File-New-Tags", null)));

    fileController.changeFileMetadata(
        new ChangeFileMetadataRequest(
            userToken, fileId, newName, newParentId, fileVisibility, fileTags));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "File metadata successfully changed");
  }

  public void handleUploadFileRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String userToken) {
    String fileName = RequestUtils.getRequiredQueryParam(request, "name");
    Optional<String> parentId = RequestUtils.getQueryParam(request, "parentId");
    List<String> fileTags =
        FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));

    // TODO: проверка на размер

    ByteBuf fileByteBuf = request.content();
    byte[] fileData = new byte[fileByteBuf.readableBytes()];
    fileByteBuf.readBytes(fileData);

    UUID createdId =
        fileController.uploadFile(
            new FileUploadRequest(parentId, fileName, userToken, fileTags, fileData));

    ResponseUtils.sendCreatedResponse(ctx, createdId, "File successfully uploaded");
  }
}
