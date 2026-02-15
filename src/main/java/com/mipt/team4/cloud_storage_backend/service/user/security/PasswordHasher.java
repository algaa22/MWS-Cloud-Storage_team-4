package com.mipt.team4.cloud_storage_backend.service.user.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {

  public static String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  public static boolean verify(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }
}
