package com.mipt.team4.cloud_storage_backend.controller.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.HealthCheckRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.HealthCheckResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ComponentStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.MemoryStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.OverallStatus;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HealthCheckController {
  private final JdbcTemplate jdbcTemplate;
  private final S3Client s3Client;
  private final ObjectMapper objectMapper;
  private final StorageConfig storageConfig;

  public void healthCheck(ChannelHandlerContext ctx, HealthCheckRequest request) {
    ComponentStatus dbStatus = ComponentStatus.OK;
    ComponentStatus s3Status = ComponentStatus.OK;
    MemoryStatus memoryStatus = MemoryStatus.OK;
    OverallStatus overallStatus = OverallStatus.UP;
    boolean isDown = false;

    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    } catch (Exception e) {
      log.error("HealthCheck: Database is down.", e);
      dbStatus = ComponentStatus.ERROR;
      overallStatus = OverallStatus.DOWN;
      isDown = true;
    }

    try {
      s3Client.listBuckets();
    } catch (Exception e) {
      log.error("HealthCheck: S3 is down.", e);
      s3Status = ComponentStatus.ERROR;
      overallStatus = OverallStatus.DOWN;
      isDown = true;
    }

    Runtime runtime = Runtime.getRuntime();
    long freeMemory = runtime.freeMemory();
    long totalMemory = runtime.totalMemory();

    if ((double) freeMemory / totalMemory < storageConfig.healthCheck().lowMemoryLimit()) {
      log.warn("HealthCheck: Low memory detected.");
      memoryStatus = MemoryStatus.LOW;
    }

    HealthCheckResponse responseDto =
        new HealthCheckResponse(overallStatus, dbStatus, s3Status, memoryStatus);

    if (isDown) {
      try {
        String jsonResponse = objectMapper.writeValueAsString(responseDto);
        ResponseUtils.sendJson(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, jsonResponse);
      } catch (Exception e) {
        ResponseUtils.sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
      }
    } else {
      ResponseUtils.send(ctx, responseDto);
    }
  }
}
