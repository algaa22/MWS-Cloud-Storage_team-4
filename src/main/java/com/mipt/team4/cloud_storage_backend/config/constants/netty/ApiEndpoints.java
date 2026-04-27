package com.mipt.team4.cloud_storage_backend.config.constants.netty;

public class ApiEndpoints {

  // Prefixes
  public static final String API_PREFIX = "/api";
  public static final String FILES_PREFIX = API_PREFIX + "/files";
  public static final String DIRECTORIES_PREFIX = API_PREFIX + "/directories";
  public static final String USERS_PREFIX = API_PREFIX + "/users";
  public static final String AUTH_PREFIX = USERS_PREFIX + "/auth";
  public static final String TARIFF_PREFIX = USERS_PREFIX + "/tariff";

  // Files (Aggregated)
  public static final String FILES_SIMPLE_UPLOAD = FILES_PREFIX + "/upload";
  public static final String FILES_RESTORE = FILES_PREFIX + "/restore";
  public static final String FILES_LIST = FILES_PREFIX + "/list";
  public static final String FILES_TRASH = FILES_PREFIX + "/trash";
  public static final String FILES_INFO = FILES_PREFIX + "/info";

  // Files (Chunked)
  public static final String FILES_CHUNKED_UPLOAD = FILES_SIMPLE_UPLOAD + "/chunked";
  public static final String FILES_CHUNKED_UPLOAD_PART = FILES_CHUNKED_UPLOAD + "/part";
  public static final String FILES_CHUNKED_UPLOAD_START = FILES_CHUNKED_UPLOAD + "/start";
  public static final String FILES_CHUNKED_UPLOAD_STATUS = FILES_CHUNKED_UPLOAD + "/status";
  public static final String FILES_CHUNKED_UPLOAD_COMPLETE = FILES_CHUNKED_UPLOAD + "/complete";
  public static final String FILES_CHUNKED_UPLOAD_ABORT = FILES_CHUNKED_UPLOAD + "/abort";
  public static final String FILES_DOWNLOAD = FILES_PREFIX + "/download";

  // Users & Auth
  public static final String AUTH_LOGIN = AUTH_PREFIX + "/login";
  public static final String AUTH_REGISTER = AUTH_PREFIX + "/register";
  public static final String AUTH_LOGOUT = AUTH_PREFIX + "/logout";
  public static final String AUTH_REFRESH = AUTH_PREFIX + "/refresh";

  // Tariffs
  public static final String TARIFF_PURCHASE = TARIFF_PREFIX + "/purchase";
  public static final String TARIFF_SET_AUTO_RENEW = TARIFF_PREFIX + "/set-auto-renew";
  public static final String TARIFF_UPDATE_PAYMENT = TARIFF_PREFIX + "/update-payment";
  public static final String TARIFF_INFO = TARIFF_PREFIX + "/info";
  public static final String TARIFF_PLANS = TARIFF_PREFIX + "/plans";
  public static final String USERS_UPDATE = USERS_PREFIX + "/update";
  public static final String USERS_INFO = USERS_PREFIX + "/info";
  public static final String SHARES_PREFIX = API_PREFIX + "/shares";
  // Shares
  public static final String SHARES_CREATE = SHARES_PREFIX;
  public static final String SHARES_GET_INFO = SHARES_PREFIX + "/info";
  public static final String SHARES_DOWNLOAD = SHARES_PREFIX + "/download";
  public static final String SHARES_VALIDATE_PASSWORD = SHARES_PREFIX + "/validate";
  public static final String SHARES_USER = SHARES_PREFIX + "/user";
  public static final String SHARES_FILE = SHARES_PREFIX + "/file";

  public static final String PAYMENTS_PREFIX = API_PREFIX + "/payments";
  public static final String PAYMENTS_HISTORY = PAYMENTS_PREFIX + "/history";
}
