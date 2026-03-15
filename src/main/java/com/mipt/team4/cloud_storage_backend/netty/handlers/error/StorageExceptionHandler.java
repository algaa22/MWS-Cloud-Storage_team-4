package com.mipt.team4.cloud_storage_backend.netty.handlers.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Sharable
@Slf4j
@RequiredArgsConstructor
public class StorageExceptionHandler extends ChannelInboundHandlerAdapter {
  private final ObjectMapper objectMapper;

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof BaseStorageException storageException) {
      if (cause instanceof ValidationFailedException validationException) {
        ResponseUtils.sendJson(
            ctx, validationException.getStatus(), createValidationFailedJson(validationException));
        return;
      }

      if (cause instanceof FatalStorageException) {
        log.error("A fatal error has been caught", cause);
        ResponseUtils.sendInternalServerErrorAndClose(ctx);
        return;
      }

      ResponseUtils.sendError(ctx, storageException.getStatus(), storageException.getMessage());
      return;
    }

    super.exceptionCaught(ctx, cause);
  }

  private ObjectNode createValidationFailedJson(ValidationFailedException exception) {
    ObjectNode rootNode = ResponseUtils.createJsonNode(false, exception.getMessage());
    rootNode.set("details", objectMapper.valueToTree(exception.getErrors()));

    return rootNode;
  }
}
