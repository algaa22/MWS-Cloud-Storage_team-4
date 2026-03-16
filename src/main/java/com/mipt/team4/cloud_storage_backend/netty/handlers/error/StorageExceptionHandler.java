package com.mipt.team4.cloud_storage_backend.netty.handlers.error;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.ErrorResponse;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Sharable
@Slf4j
@RequiredArgsConstructor
public class StorageExceptionHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause = unwrapCause(cause);

    if (!(cause instanceof BaseStorageException exception)) {
      ctx.fireExceptionCaught(cause);
      return;
    }

    Object details = null;

    if (cause instanceof ValidationFailedException ve) {
      details = ve.getErrors();
    }

    if (cause instanceof FatalStorageException) {
      log.error("A fatal error has been caught", cause);
      sendErrorAndClose(
          ctx,
          new ErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("A business error has been caught", cause);
    }

    ResponseUtils.send(
        ctx, new ErrorResponse(exception.getStatus(), exception.getMessage(), details));
  }

  private void sendErrorAndClose(ChannelHandlerContext ctx, ErrorResponse error) {
    ResponseUtils.send(ctx, error).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
  }

  private Throwable unwrapCause(Throwable cause) {
    if (cause instanceof CodecException
        || cause instanceof InvocationTargetException
        || cause instanceof ExecutionException) {
      if (cause.getCause() != null) {
        return unwrapCause(cause.getCause());
      }
    }

    return cause;
  }
}
