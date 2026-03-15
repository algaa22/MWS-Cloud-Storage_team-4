package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.MoveDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {

  private final DirectoryService service;
  private final AccessTokenService accessTokenService;

  public UUID createDirectory(CreateDirectoryRequest request) {
    request.validate(accessTokenService);
    return service.createDirectory(request);
  }

  public void renameDirectory(RenameDirectoryRequest request) {
    request.validate(accessTokenService);
    service.renameDirectory(request);
  }

  public void moveDirectory(MoveDirectoryRequest request) {
    request.validate(accessTokenService);
    service.moveDirectory(request);
  }

  public void deleteDirectory(DeleteDirectoryRequest request) {
    request.validate(accessTokenService);
    service.deleteDirectory(request);
  }
}
