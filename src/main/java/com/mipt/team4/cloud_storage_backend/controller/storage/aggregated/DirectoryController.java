package com.mipt.team4.cloud_storage_backend.controller.storage.aggregated;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UpdateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {
  private final DirectoryService directoryService;

  public void createDirectory(ChannelHandlerContext ctx, CreateDirectoryRequest request) {
    UUID createdId = directoryService.createDirectory(request);
    ResponseUtils.sendCreatedResponse(ctx, createdId, "Directory successfully created");
  }

  public void updateDirectory(ChannelHandlerContext ctx, UpdateDirectoryRequest request) {
    if (request.newParentId() != null) {
      directoryService.moveDirectory(request);
    } else {
      directoryService.renameDirectory(request);
    }

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Directory successfully updated");
  }

  public void deleteDirectory(ChannelHandlerContext ctx, DeleteDirectoryRequest request) {
    directoryService.deleteDirectory(request);
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Directory successfully deleted");
  }
}
