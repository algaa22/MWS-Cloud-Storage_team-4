package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import java.util.UUID;

public class UserNotFoundException extends FatalStorageException {

  public UserNotFoundException(UUID userId) {
    super("User with ID " + userId + " was not found");
  }
}
