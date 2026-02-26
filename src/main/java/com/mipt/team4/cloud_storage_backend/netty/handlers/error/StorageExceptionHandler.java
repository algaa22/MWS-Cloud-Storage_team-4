package com.mipt.team4.cloud_storage_backend.netty.handlers.error;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
public class StorageExceptionHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof BaseStorageException storageException) {
      if (cause instanceof FatalStorageException) {
        log.error("A fatal error has been caught", cause);
        ResponseUtils.sendInternalServerErrorAndClose(ctx);
        return;
      }

      ResponseUtils.sendError(ctx, storageException.getStatus(), storageException.getMessage());
      return;
    }

    ctx.fireExceptionCaught(cause);
  }
}
