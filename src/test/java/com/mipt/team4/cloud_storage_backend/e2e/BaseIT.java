package com.mipt.team4.cloud_storage_backend.e2e;

import java.net.http.HttpClient;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2ETestSetupExtension.class)
public class BaseIT {

  protected static final HttpClient client = HttpClient.newHttpClient();
}