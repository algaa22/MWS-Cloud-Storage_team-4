package com.mipt.team4.cloud_storage_backend.netty.handlers.validation;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RoutedMessage;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class GlobalValidationHandler extends SimpleChannelInboundHandler<Object> {
  private final Validator validator;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof RoutedMessage) {
      Set<ConstraintViolation<Object>> violations = validator.validate(msg);

      if (!violations.isEmpty()) {
        Set<ValidationError> errors =
            violations.stream()
                .map(v -> new ValidationError(getFieldName(v), v.getMessage()))
                .collect(Collectors.toSet());

        throw new ValidationFailedException(errors);
      }
    }

    ctx.fireChannelRead(msg);
  }

  private String getFieldName(ConstraintViolation<Object> violation) {
    String fieldName = "";

    for (Path.Node node : violation.getPropertyPath()) {
      fieldName = node.getName();
    }

    return fieldName;
  }
}
