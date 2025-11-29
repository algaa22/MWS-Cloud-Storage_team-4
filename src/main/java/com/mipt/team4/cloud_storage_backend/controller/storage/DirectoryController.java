package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeDirectoryPathDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleDirectoryOperationDto;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import java.io.FileNotFoundException;

public class DirectoryController {
  private final DirectoryService service;

  public DirectoryController(DirectoryService service) {
    this.service = service;
  }

  public void createDirectory(SimpleDirectoryOperationDto request)
      throws ValidationFailedException, UserNotFoundException, StorageFileAlreadyExistsException {
    request.validate();
    service.createDirectory(request);
  }

  public void changeDirectoryPath(ChangeDirectoryPathDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageFileNotFoundException {
    request.validate();
    service.changeDirectoryPath(request);
  }

  public void deleteDirectory(SimpleDirectoryOperationDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileNotFoundException,
          FileNotFoundException {
    request.validate();
    service.deleteDirectory(request);
  }
}
