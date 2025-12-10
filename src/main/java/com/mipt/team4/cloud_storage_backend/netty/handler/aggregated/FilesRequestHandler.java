package com.mipt.team4.cloud_storage_backend.netty.handler.aggregated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public record FilesRequestHandler(FileController fileController) {
  // TODO: параметры vs заголовки

  public void handleGetFilePathsListRequest(
      ChannelHandlerContext ctx, HttpRequest request, String userToken)
      throws UserNotFoundException, ValidationFailedException {
    // TODO: пагинация
    boolean includeDirectories =
        SafeParser.parseBoolean(
            "Include directories",
            RequestUtils.getQueryParam(request, "includeDirectories", "false"));
    boolean recursive =
            SafeParser.parseBoolean(
                    "Recursive",
                    RequestUtils.getQueryParam(request, "recursive", "false"));
    Optional<String> searchDirectory = RequestUtils.getQueryParam(request, "directory");

    List<StorageEntity> files =
        fileController.getFileList(
            new GetFileListDto(userToken, includeDirectories, recursive, searchDirectory));

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    ArrayNode filesArray = mapper.createArrayNode();

    if (files != null) {
      for (StorageEntity file : files) {
        ObjectNode fileNode = mapper.createObjectNode();
        fileNode.put("path", file.getPath());
        filesArray.add(fileNode);
      }
    }

    rootNode.set("files", filesArray);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleGetFileInfoRequest(ChannelHandlerContext ctx, String filePath, String userToken)
      throws UserNotFoundException, StorageEntityNotFoundException, ValidationFailedException {
    StorageDto storageDto =
        fileController.getFileInfo(new SimpleFileOperationDto(filePath, userToken));

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Path", storageDto.path());
    rootNode.put("Type", storageDto.type());
    rootNode.put("Visibility", storageDto.visibility());
    rootNode.put("Size", storageDto.size());
    rootNode.put("IsDeleted", storageDto.isDeleted());
    rootNode.put("Tags", FileTagsMapper.toString(storageDto.tags()));

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleDeleteFileRequest(ChannelHandlerContext ctx, String filePath, String userToken)
      throws UserNotFoundException,
          StorageEntityNotFoundException,
          ValidationFailedException,
          StorageIllegalAccessException,
          FileNotFoundException {
    fileController.deleteFile(new SimpleFileOperationDto(filePath, userToken));

    ResponseHelper.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully deleted");
  }

  public void handleChangeFileMetadataRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String filePath, String userToken)
      throws UserNotFoundException,
          StorageEntityNotFoundException,
          StorageFileAlreadyExistsException,
          ValidationFailedException {
    Optional<String> newFilePath = RequestUtils.getQueryParam(request, "newPath");

    Optional<String> fileVisibility =
        Optional.ofNullable(RequestUtils.getHeader(request, "X-File-New-Visibility", null));

    Optional<List<String>> fileTags =
        Optional.ofNullable(
            FileTagsMapper.toList(RequestUtils.getHeader(request, "X-File-New-Tags", null)));

    fileController.changeFileMetadata(
        new ChangeFileMetadataDto(userToken, filePath, newFilePath, fileVisibility, fileTags));

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "File metadata successfully changed");
  }

  public void handleUploadFileRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String filePath, String userToken)
      throws HeaderNotFoundException,
          StorageFileAlreadyExistsException,
          UserNotFoundException,
          ValidationFailedException {
    List<String> fileTags =
        FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));

    // TODO: проверка на размер

    ByteBuf fileByteBuf = request.content();
    byte[] fileData = new byte[fileByteBuf.readableBytes()];
    fileByteBuf.readBytes(fileData);

    fileController.uploadFile(new FileUploadDto(filePath, userToken, fileTags, fileData));

    ResponseHelper.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully uploaded");
  }

  public void handleDownloadFileRequest(
      ChannelHandlerContext ctx, String filePath, String userToken)
      throws UserNotFoundException,
          StorageEntityNotFoundException,
          ValidationFailedException,
          IOException {
    FileDownloadDto fileDownload =
        fileController.downloadFile(new SimpleFileOperationDto(filePath, userToken));

    try (InputStream fileStream = fileDownload.fileStream()) {
      ResponseHelper.sendBinaryResponse(ctx, fileDownload.mimeType(), fileStream.readAllBytes());
    }
  }
}
