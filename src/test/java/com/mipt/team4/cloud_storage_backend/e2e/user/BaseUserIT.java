package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.e2e.BaseIT;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.TestUserDto;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserAuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserIT extends BaseIT {
  @Autowired private UserAuthUtils userAuthUtils;

  protected String testEmail;
  protected String testPassword;

  @BeforeEach
  public void beforeEach() {
    TestUserDto testUser = userAuthUtils.createRandomUser();

    testEmail = testUser.email();
    testPassword = testUser.password();
  }
}
