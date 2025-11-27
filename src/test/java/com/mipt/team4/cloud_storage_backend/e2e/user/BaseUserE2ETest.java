package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;

public abstract class BaseUserE2ETest extends BaseE2ETest {

  protected final String TEST_EMAIL = "test_user@example.com";
  protected final String TEST_PASSWORD = "superpassword12345";

  @BeforeEach
  public void setup() {
  }
}



