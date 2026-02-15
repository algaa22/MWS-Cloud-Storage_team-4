package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeDirectoryPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleDirectoryOperationDto;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import java.io.FileNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
public class DirectoryController {

  private final DirectoryService service;
  private final JwtService jwtService;

  public DirectoryController(DirectoryService service, JwtService jwtService) {
    this.service = service;
    this.jwtService = jwtService;
  }

  public void createDirectory(SimpleDirectoryOperationDto request)
      throws ValidationFailedException, UserNotFoundException, StorageFileAlreadyExistsException {
    request.validate(jwtService);
    service.createDirectory(request);
  }

  public void changeDirectoryPath(ChangeDirectoryPathDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageEntityNotFoundException {
    request.validate(jwtService);
    service.changeDirectoryPath(request);
  }

  public void deleteDirectory(SimpleDirectoryOperationDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          FileNotFoundException {
    request.validate(jwtService);
    service.deleteDirectory(request);
  }
}
