package com.mipt.team4.cloud_storage_backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.nio.charset.StandardCharsets;

public class UserController {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }


}
