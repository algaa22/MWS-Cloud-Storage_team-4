package com.mipt.team4.cloud_storage_backend.netty.constants;

public class ValidationPatterns {
  public static final String FILE_NAME_REGEXP = "^[^\\\\/:*?\"<>|]+$";
  public static final String FILE_NAME_ERROR = "File name contains forbidden characters";

  public static final String SINGLE_TAG_REGEXP = "^[a-zA-Z0-9а-яА-Я]+$";
  public static final String SINGLE_TAG_ERROR =
      "Each tag must be alphanumeric and contain no spaces";

  public static final String EMAIL_REGEXP = "^[A-Za-z0-9+_.-]+@(.+)$";
  public static final String EMAIL_ERROR = "Invalid email format";

  public static final String PASSWORD_REGEXP =
      "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,64}$";
  public static final String PASSWORD_ERROR =
      "Password must be between 8 and 64 characters long and include uppercase and lowercase letters, "
          + "digits, and special characters (@#$%^&+=!_-)";

  public static final String PAYMENT_TOKEN_REGEXP = "^[a-zA-Z0-9_.-]+$";
  public static final String PAYMENT_TOKEN_ERROR =
      "Invalid payment token format. Only alphanumeric characters, dots, and dashes are allowed.";
}
