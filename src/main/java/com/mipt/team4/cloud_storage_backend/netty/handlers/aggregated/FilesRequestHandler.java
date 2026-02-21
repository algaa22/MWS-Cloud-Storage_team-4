package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
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
import java.io.FileNotFoundException;
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

  public void handleGetFilePathsListRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException, ValidationFailedException {
    boolean includeDirectories =
        SafeParser.parseBoolean(
            "Include directories",
            RequestUtils.getQueryParam(request, "includeDirectories", "false"));
    boolean recursive =
        SafeParser.parseBoolean(
            "Recursive", RequestUtils.getQueryParam(request, "recursive", "false"));
    Optional<UUID> parentId =
        Optional.ofNullable(RequestUtils.getOptionalUuidQueryParam(request, "parentId"));

    List<StorageEntity> files =
        fileController.getFileList(
            new GetFileListRequest(userToken, includeDirectories, recursive, parentId));

    ObjectNode rootNode = mapper.createObjectNode();
    ArrayNode filesArray = mapper.createArrayNode();

    if (files != null) {
      for (StorageEntity file : files) {
        ObjectNode fileNode = mapper.createObjectNode();
        fileNode.put("id", file.getId().toString());
        fileNode.put("name", file.getName());
        fileNode.put("isDirectory", file.isDirectory());
        fileNode.put("size", file.getSize());
        filesArray.add(fileNode);
      }
    }

    rootNode.set("files", filesArray);

    ResponseUtils.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleGetFileInfoRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException {
    UUID parentId = RequestUtils.getOptionalUuidQueryParam(request, "parentId");
    String name = RequestUtils.getRequiredQueryParam(request, "name");
    StorageDto storageDto =
        fileController.getFileInfo(new SimpleFileOperationRequest(parentId, name, userToken));

    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Id", storageDto.storageId().toString());
    rootNode.put("Name", storageDto.name());
    rootNode.put(
        "ParentId", storageDto.parentId() != null ? storageDto.parentId().toString() : null);
    rootNode.put("Type", storageDto.type());
    rootNode.put("Visibility", storageDto.visibility());
    rootNode.put("Size", storageDto.size());
    rootNode.put("IsDeleted", storageDto.isDeleted());
    rootNode.set("Tags", mapper.valueToTree(storageDto.tags()));

    ResponseUtils.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleDeleteFileRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          FileNotFoundException,
          QueryParameterNotFoundException {

    UUID parentId = RequestUtils.getOptionalUuidQueryParam(request, "parentId");
    String name = RequestUtils.getRequiredQueryParam(request, "name");
    fileController.deleteFile(new SimpleFileOperationRequest(parentId, name, userToken));

    ResponseUtils.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully deleted");
  }

  public void handleChangeFileMetadataRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String userToken)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException,
          ValidationFailedException,
          QueryParameterNotFoundException {
    UUID fileId = RequestUtils.getRequiredUuidQueryParam(request, "id");

    Optional<String> newName = RequestUtils.getQueryParam(request, "newName");
    Optional<UUID> newParentId =
        Optional.ofNullable(RequestUtils.getOptionalUuidQueryParam(request, "newParentId"));

    Optional<String> fileVisibility =
        Optional.ofNullable(RequestUtils.getHeader(request, "X-File-New-Visibility", null));

    Optional<List<String>> fileTags =
        Optional.ofNullable(
            FileTagsMapper.toList(RequestUtils.getHeader(request, "X-File-New-Tags", null)));

    fileController.changeFileMetadata(
        new ChangeFileMetadataRequest(
            userToken, fileId, newName, newParentId, fileVisibility, fileTags));

    ResponseUtils.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "File metadata successfully changed");
  }

  public void handleUploadFileRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String userToken)
      throws HeaderNotFoundException,
          StorageFileAlreadyExistsException,
          UserNotFoundException,
          ValidationFailedException,
          QueryParameterNotFoundException {

    String fileName = RequestUtils.getRequiredQueryParam(request, "name");
    UUID parentId = RequestUtils.getOptionalUuidQueryParam(request, "parentId");
    List<String> fileTags =
        FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));

    // TODO: проверка на размер

    ByteBuf fileByteBuf = request.content();
    byte[] fileData = new byte[fileByteBuf.readableBytes()];
    fileByteBuf.readBytes(fileData);

    fileController.uploadFile(
        new FileUploadRequest(parentId, fileName, userToken, fileTags, fileData));

    ResponseUtils.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully uploaded");
  }
}
