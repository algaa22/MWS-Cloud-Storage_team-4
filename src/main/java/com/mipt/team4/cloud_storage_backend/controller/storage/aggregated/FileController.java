package com.mipt.team4.cloud_storage_backend.controller.storage.aggregated;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.CreatedResponse;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.common.mappers.PaginationMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.TrashFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileInfoResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileListResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.channel.ChannelHandlerContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FileController {
  private final FileService fileService;

  public void getFileList(ChannelHandlerContext ctx, GetFileListRequest request) {
    Page<StorageEntity> files = fileService.getFileList(request);

    FileListResponse response =
        new FileListResponse(
            PaginationMapper.toResponse(
                files, files.stream().map(FileInfoResponse::from).toList()));

    ResponseUtils.send(ctx, response);
  }

  public void getTrashFileList(ChannelHandlerContext ctx, TrashFileListRequest request) {
    Page<StorageEntity> trashFiles = fileService.getTrashFileList(request);
    FileListResponse response =
        new FileListResponse(
            PaginationMapper.toResponse(
                trashFiles, trashFiles.stream().map(FileInfoResponse::from).toList()));
    ResponseUtils.send(ctx, response);
  }

  public void getFileInfo(ChannelHandlerContext ctx, GetFileInfoRequest request) {
    StorageEntity entity = fileService.getInfo(request);
    FileInfoResponse response = FileInfoResponse.from(entity);
    ResponseUtils.send(ctx, response);
  }

  public void uploadFile(ChannelHandlerContext ctx, SimpleUploadRequest request) {
    UUID createdId = fileService.simpleUpload(request);
    ResponseUtils.send(ctx, new CreatedResponse(createdId, "File successfully uploaded"));
  }

  public void deleteFile(ChannelHandlerContext ctx, DeleteFileRequest request) {
    if (request.permanent()) {
      fileService.hardDelete(request);
    } else {
      fileService.softDelete(request);
    }

    ResponseUtils.send(ctx, new SuccessResponse("File successfully deleted"));
  }

  public void restoreFile(ChannelHandlerContext ctx, RestoreFileRequest request) {
    fileService.restore(request);
    ResponseUtils.send(ctx, new SuccessResponse("File successfully restored"));
  }

  public void changeMetadata(ChannelHandlerContext ctx, ChangeFileMetadataRequest request) {
    fileService.changeMetadata(request);
    ResponseUtils.send(ctx, new SuccessResponse("File metadata successfully changed"));
  }
}
