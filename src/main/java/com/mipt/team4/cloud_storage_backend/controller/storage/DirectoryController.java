package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.MoveDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {

  private final DirectoryService service;
  private final JwtService jwtService;

  public UUID createDirectory(CreateDirectoryRequest request) {
    request.validate(jwtService);
    return service.createDirectory(request);
  }

  public void renameDirectory(RenameDirectoryRequest request) {
    request.validate(jwtService);
    service.renameDirectory(request);
  }

  public void moveDirectory(MoveDirectoryRequest request) {
    request.validate(jwtService);
    service.moveDirectory(request);
  }

  public void deleteDirectory(DeleteDirectoryRequest request) {
    request.validate(jwtService);
    service.deleteDirectory(request);
  }
}
