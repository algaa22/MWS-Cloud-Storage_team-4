package com.mipt.team4.cloud_storage_backend.netty.constants;

import io.netty.util.AttributeKey;
import java.util.UUID;

public class SecurityAttributes {
  public static final AttributeKey<UUID> USER_ID = AttributeKey.valueOf("userId");
}
