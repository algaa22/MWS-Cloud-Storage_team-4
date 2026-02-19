package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeDirectoryPathRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleDirectoryOperationRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import java.io.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {

  private final DirectoryService service;
  private final JwtService jwtService;

  public void createDirectory(SimpleDirectoryOperationRequest request)
      throws ValidationFailedException, UserNotFoundException, StorageFileAlreadyExistsException {
    request.validate(jwtService);
    service.createDirectory(request);
  }

  public void changeDirectoryPath(ChangeDirectoryPathRequest request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
      StorageFileNotFoundException {
    request.validate(jwtService);
    service.changeDirectoryPath(request);
  }

  public void deleteDirectory(SimpleDirectoryOperationRequest request)
      throws ValidationFailedException,
          UserNotFoundException,
      StorageFileNotFoundException,
          FileNotFoundException {
    request.validate(jwtService);
    service.deleteDirectory(request);
  }
}
