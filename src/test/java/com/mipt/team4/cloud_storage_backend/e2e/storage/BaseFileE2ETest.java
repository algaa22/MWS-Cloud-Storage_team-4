package com.mipt.team4.cloud_storage_backend.e2e.storage;

import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

public class BaseFileE2ETest extends BaseE2ETest {
  protected String currentUserToken;

  @BeforeEach
  public void beforeEach() {
    currentUserToken =
            UserAuthUtils.sendRegisterTestUserRequest(
                    client,
                    new Random().nextInt(10000) + "@email.com",
                    "deadlyparkourkillerdarkbrawlstarsassassinstalkersniper1998rus",
                    "superpassword1488");
  }
}
