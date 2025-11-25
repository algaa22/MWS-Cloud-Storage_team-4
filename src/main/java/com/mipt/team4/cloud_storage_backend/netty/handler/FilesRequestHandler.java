package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

public record FilesRequestHandler(FileController fileController) {
  // TODO: параметры vs заголовки

  public void handleGetFilePathsListRequest(ChannelHandlerContext ctx, String userToken) {
    List<String> paths;

    try {
      paths = fileController.getFilePathsList(new GetFilePathsListDto(userToken));
    } catch (ValidationFailedException | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

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
      ChannelHandlerContext ctx, String filePath, String userToken) {
    FileDto fileDto;

    try {
      fileDto = fileController.getFileInfo(new SimpleFileOperationDto(filePath, userToken));
    } catch (ValidationFailedException | StorageFileNotFoundException | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Path", fileDto.path());
    rootNode.put("Type", fileDto.type());
    rootNode.put("Visibility", fileDto.visibility());
    rootNode.put("Size", fileDto.size());
    rootNode.put("IsDeleted", fileDto.isDeleted());
    rootNode.put("Tags", FileTagsMapper.toString(fileDto.tags()));

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleDeleteFileRequest(ChannelHandlerContext ctx, String fileId, String userToken) {
    try {
      fileController.deleteFile(new SimpleFileOperationDto(fileId, userToken));
    } catch (UserNotFoundException
        | ValidationFailedException
        | StorageIllegalAccessException
        | StorageFileNotFoundException
        | FileNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
    }

    ResponseHelper.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully deleted");
  }

  public void handleChangeFileMetadataRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String filePath, String userToken) {
    Optional<String> newFilePath = RequestUtils.getHeader(request, "X-File-New-Path");

    Optional<String> fileVisibility =
        Optional.ofNullable(RequestUtils.getHeader(request, "X-File-Visibility", null));

    Optional<List<String>> fileTags =
        Optional.ofNullable(
            FileTagsMapper.toList(RequestUtils.getHeader(request, "X-File-Tags", null)));

    try {
      fileController.changeFileMetadata(
          new ChangeFileMetadataDto(userToken, filePath, newFilePath, fileVisibility, fileTags));
    } catch (ValidationFailedException
        | UserNotFoundException
        | StorageFileNotFoundException
        | StorageFileAlreadyExistsException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "File metadata successfully changed");
  }

  public void handleUploadFileRequest(
      ChannelHandlerContext ctx, FullHttpRequest request, String filePath, String userToken) {
    List<String> fileTags;

    try {
      fileTags = FileTagsMapper.toList(RequestUtils.getRequiredHeader(request, "X-File-Tags"));
    } catch (HeaderNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    // TODO: проверка на размер

    ByteBuf fileByteBuf = request.content();

    // TODO: correct?
    if (request.content().readableBytes() == 0) {
      ResponseHelper.sendErrorResponse(
          ctx, HttpResponseStatus.BAD_REQUEST, "Upload request does not contain content");
      return;
    }

    byte[] fileData = new byte[fileByteBuf.readableBytes()];
    fileByteBuf.readBytes(fileData);

    try {
      fileController.uploadFile(new FileUploadDto(filePath, userToken, fileTags, fileData));
    } catch (UserNotFoundException
        | StorageFileAlreadyExistsException
        | ValidationFailedException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(ctx, HttpResponseStatus.OK, "File successfully uploaded");
  }

  // TODO: выбор метода скачивания

  public void handleDownloadFileRequest(
      ChannelHandlerContext ctx, String filePath, String userToken) {
    FileDownloadDto fileDownload;

    try {
      fileDownload = fileController.downloadFile(new SimpleFileOperationDto(filePath, userToken));
    } catch (UserNotFoundException
        | StorageIllegalAccessException
        | ValidationFailedException
        | FileNotFoundException
        | StorageFileNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendBinaryResponse(ctx, fileDownload.mimeType(), fileDownload.data());
  }
}
