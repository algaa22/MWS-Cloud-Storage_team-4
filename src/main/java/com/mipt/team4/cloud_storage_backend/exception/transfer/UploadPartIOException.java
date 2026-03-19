package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;

public class UploadPartIOException extends FatalStorageException {
  public UploadPartIOException(ChannelHandlerContext ctx, IOException cause) {
    super("Failed to upload part in channel: " + ctx.channel().remoteAddress(), cause);
  }
}
