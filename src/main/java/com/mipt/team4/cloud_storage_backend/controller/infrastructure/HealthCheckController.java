package com.mipt.team4.cloud_storage_backend.controller.infrastructure;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.HealthCheckRequest;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.infrastructure.HealthMonitorService;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HealthCheckController {

  private final HealthMonitorService healthMonitorService;

  public void healthCheck(ChannelHandlerContext ctx, HealthCheckRequest request) {
    ResponseUtils.send(ctx, healthMonitorService.getCurrentHealth());
  }
}
