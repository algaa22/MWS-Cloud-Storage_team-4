package com.mipt.team4.cloud_storage_backend.netty.constants;

public class ValidationConstants {
  public static final String FILE_NAME_REGEXP = "^[^\\\\/:*?\"<>|]+$";
  public static final String FILE_NAME_ERROR = "File name contains forbidden characters";

  public static final String SINGLE_TAG_REGEXP = "^[a-zA-Z0-9а-яА-Я]+$";
  public static final String SINGLE_TAG_ERROR =
      "Each tag must be alphanumeric and contain no spaces";

  public static final String EMAIL_REGEXP = "^[A-Za-z0-9+_.-]+@(.+)$";
  public static final String EMAIL_ERROR = "Invalid email format";

  public static final String PASSWORD_REGEXP = "^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$";
  public static final String PASSWORD_ERROR =
      "Password must be at least 8 characters long and contain both letters and numbers";

  public static final String PAYMENT_TOKEN_REGEXP = "^[a-zA-Z0-9_.-]+$";
  public static final String PAYMENT_TOKEN_ERROR =
      "Invalid payment token format. Only alphanumeric characters, dots, and dashes are allowed.";
}
