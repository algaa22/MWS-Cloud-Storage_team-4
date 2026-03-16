package com.mipt.team4.cloud_storage_backend.netty.constants;

public class ApiEndpoints {

  // Префиксы модулей
  public static final String API_PREFIX = "/api";
  public static final String FILES_PREFIX = API_PREFIX + "/files";
  public static final String DIRECTORIES_PREFIX = API_PREFIX + "/directories";
  public static final String USERS_PREFIX = API_PREFIX + "/users";
  public static final String AUTH_PREFIX = USERS_PREFIX + "/auth";
  public static final String TARIFF_PREFIX = USERS_PREFIX + "/tariff";

  // Files (Aggregated)
  public static final String FILES_SIMPLE_UPLOAD = FILES_PREFIX + "/upload";
  public static final String FILES_CHUNKED_UPLOAD = FILES_SIMPLE_UPLOAD + "/chunked";
  public static final String FILES_RESTORE = FILES_PREFIX + "/restore";
  public static final String FILES_LIST = FILES_PREFIX + "/list";
  public static final String FILES_TRASH = FILES_PREFIX + "/trash";
  public static final String FILES_INFO = FILES_PREFIX + "/info";

  // Files (Chunked)
  public static final String FILES_CHUNKED_UPLOAD_RESUME = FILES_CHUNKED_UPLOAD + "/resume";
  public static final String FILES_DOWNLOAD = FILES_PREFIX + "/download";

  // Users & Auth
  public static final String AUTH_LOGIN = AUTH_PREFIX + "/login";
  public static final String AUTH_REGISTER = AUTH_PREFIX + "/register";
  public static final String AUTH_LOGOUT = AUTH_PREFIX + "/logout";
  public static final String AUTH_REFRESH = AUTH_PREFIX + "/refresh";
  public static final String USERS_UPDATE = USERS_PREFIX + "/update";
  public static final String USERS_INFO = USERS_PREFIX + "/info";

  // Tariffs
  public static final String TARIFF_PURCHASE = TARIFF_PREFIX + "/purchase";
  public static final String TARIFF_SET_AUTO_RENEW = TARIFF_PREFIX + "/set-auto-renew";
  public static final String TARIFF_UPDATE_PAYMENT = TARIFF_PREFIX + "/update-payment";
  public static final String TARIFF_INFO = TARIFF_PREFIX + "/info";
  public static final String TARIFF_PLANS = TARIFF_PREFIX + "/plans";
}
