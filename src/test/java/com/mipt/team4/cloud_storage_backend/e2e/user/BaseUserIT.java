package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.e2e.BaseIT;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.TestUserDto;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserAuthUtils;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseUserIT extends BaseIT {

  protected String testEmail;
  protected String testPassword;

  @BeforeEach
  public void beforeEach() {
    TestUserDto testUser = UserAuthUtils.createRandomUser();

    testEmail = testUser.email();
    testPassword = testUser.password();
  }
}