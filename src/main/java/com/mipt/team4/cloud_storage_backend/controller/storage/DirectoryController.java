package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeDirectoryPathRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleDirectoryOperationRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {

  private final DirectoryService service;
  private final JwtService jwtService;

  public void createDirectory(SimpleDirectoryOperationRequest request) {
    request.validate(jwtService);
    service.createDirectory(request);
  }

  public void changeDirectoryPath(ChangeDirectoryPathRequest request) {
    request.validate(jwtService);
    service.changeDirectoryPath(request);
  }

  public void deleteDirectory(SimpleDirectoryOperationRequest request) {
    request.validate(jwtService);
    service.deleteDirectory(request);
  }
}
