package com.mipt.team4.cloud_storage_backend.config.constants.netty;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartContext;
import io.netty.util.AttributeKey;
import java.util.UUID;

public class NettyAttributes {
  public static final AttributeKey<UUID> USER_ID = AttributeKey.valueOf("user_id");
  public static final AttributeKey<Boolean> IGNORABLE_ERROR_LOGGED =
      AttributeKey.valueOf("ignorable_error_logged");
  public static final AttributeKey<ChunkedUploadPartContext> CHUNKED_UPLOAD_PART_INFO =
      AttributeKey.valueOf("chunked_upload_part_info");
}
