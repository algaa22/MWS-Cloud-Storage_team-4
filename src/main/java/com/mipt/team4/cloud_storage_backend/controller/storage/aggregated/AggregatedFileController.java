package com.mipt.team4.cloud_storage_backend.controller.storage.aggregated;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.CreatedResponse;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.TrashFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileInfoResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileListResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AggregatedFileController {
  private final FileService fileService;

  public void getFileList(ChannelHandlerContext ctx, GetFileListRequest request) {
    List<StorageEntity> files = fileService.getFileList(request);

    FileListResponse response =
        new FileListResponse(files.stream().map(FileInfoResponse::from).toList());

    ResponseUtils.send(ctx, response);
  }

  public void getTrashFileList(ChannelHandlerContext ctx, TrashFileListRequest request) {
    List<StorageEntity> trashFiles = fileService.getAllTrashFiles(request.userId());

    FileListResponse response =
            new FileListResponse(trashFiles.stream().map(FileInfoResponse::from).toList());
    ResponseUtils.send(ctx, response);
  }

  public void getFileInfo(ChannelHandlerContext ctx, GetFileInfoRequest request) {
    StorageEntity entity = fileService.getInfo(request);
    FileInfoResponse response = FileInfoResponse.from(entity);
    ResponseUtils.send(ctx, response);
  }

  public void uploadFile(ChannelHandlerContext ctx, FileUploadRequest request) {
    UUID createdId = fileService.simpleUpload(request);
    ResponseUtils.send(ctx, new CreatedResponse(createdId, "File successfully uploaded"));
  }

  public void deleteFile(ChannelHandlerContext ctx, DeleteFileRequest request) {
    System.out.println("=== DELETE CONTROLLER ===");
    System.out.println("File ID: " + request.id());
    System.out.println("Permanent: " + request.permanent());  // Должно быть true!

    if (request.permanent()) {
      System.out.println("🔥 CALLING HARD DELETE");
      fileService.hardDelete(request);
    } else {
      System.out.println("📦 CALLING SOFT DELETE");
      fileService.softDelete(request);
    }

    ResponseUtils.send(ctx, new SuccessResponse("File successfully deleted"));
  }

  public void restoreFile(ChannelHandlerContext ctx, RestoreFileRequest request) {
    System.out.println("=== RESTORE CONTROLLER ===");
    System.out.println("User ID: " + request.userId());
    System.out.println("File ID: " + request.fileId());
    System.out.println("Timestamp: " + new java.util.Date());

    try {
      fileService.restore(request);
      System.out.println("Restore successful");
      ResponseUtils.send(ctx, new SuccessResponse("File successfully restored"));
    } catch (Exception e) {
      System.out.println("Restore failed: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  public void changeMetadata(ChannelHandlerContext ctx, ChangeFileMetadataRequest request) {
    fileService.changeMetadata(request);
    ResponseUtils.send(ctx, new SuccessResponse("File metadata successfully changed"));
  }
}
