package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.UUID;

public class UserAlreadyExistsException extends BaseStorageException {

  public UserAlreadyExistsException(UUID id) {
    super("User with ID " + id + " already exists", HttpResponseStatus.CONFLICT);
  }

  public UserAlreadyExistsException(String email) {
    super("User with email " + email + " already exists", HttpResponseStatus.CONFLICT);
  }
}
