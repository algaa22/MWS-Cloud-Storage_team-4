package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CreateDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.MoveDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RenameDirectoryRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectoryController {

  private final DirectoryService service;
  private final JwtService jwtService;

  public void createDirectory(CreateDirectoryRequest request)
      throws ValidationFailedException, UserNotFoundException, StorageFileAlreadyExistsException {
    request.validate(jwtService);
    service.createDirectory(request);
  }

  public void renameDirectory(RenameDirectoryRequest request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageFileNotFoundException {
    request.validate(jwtService);
    service.renameDirectory(request.userToken(), request.directoryId(), request.newName());
  }

  public void moveDirectory(MoveDirectoryRequest request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileAlreadyExistsException,
          StorageFileNotFoundException {
    request.validate(jwtService);
    service.moveDirectory(request.userToken(), request.directoryId(), request.newParentId());
  }

  public void deleteDirectory(DeleteDirectoryRequest request)
      throws ValidationFailedException, UserNotFoundException, StorageFileNotFoundException {
    request.validate(jwtService);
    service.deleteDirectory(request.userToken(), request.directoryId());
  }
}
