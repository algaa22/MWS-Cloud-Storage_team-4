package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseUserE2ETest extends BaseE2ETest {

  protected String testEmail;
  protected String testPassword;

  @BeforeEach
  public void beforeEach() {
    TestUserDto testUser = UserAuthUtils.createRandomUser();

    testEmail = testUser.email();
    testPassword = testUser.password();
  }
}



