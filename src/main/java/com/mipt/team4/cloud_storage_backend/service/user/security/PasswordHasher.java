package com.mipt.team4.cloud_storage_backend.service.user.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

  public String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  public boolean verify(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }
}
