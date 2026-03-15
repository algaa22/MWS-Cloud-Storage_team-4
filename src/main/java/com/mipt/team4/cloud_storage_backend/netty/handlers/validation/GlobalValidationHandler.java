package com.mipt.team4.cloud_storage_backend.netty.handlers.validation;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RouteRegistry;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class GlobalValidationHandler extends SimpleChannelInboundHandler<Object> {
  private final RouteRegistry routeRegistry;
  private final Validator validator;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (routeRegistry.isRegisteredDto(msg.getClass())) {
      Set<ConstraintViolation<Object>> violations = validator.validate(msg);

      if (!violations.isEmpty()) {
        Set<ValidationError> errors =
            violations.stream()
                .map(v -> new ValidationError(v.getPropertyPath().toString(), v.getMessage()))
                .collect(Collectors.toSet());

        throw new ValidationFailedException(errors);
      }
    }

    ctx.fireChannelRead(msg);
  }
}
