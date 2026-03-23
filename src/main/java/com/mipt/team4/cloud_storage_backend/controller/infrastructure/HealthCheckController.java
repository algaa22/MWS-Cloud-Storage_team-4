package com.mipt.team4.cloud_storage_backend.controller.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.HealthCheckRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.HealthCheckResponse;
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

  public void healthCheck(ChannelHandlerContext ctx, HealthCheckRequest request) {
    String dbStatus = "OK";
    String s3Status = "OK";
    String memoryStatus = "OK";
    String overallStatus = "UP";
    boolean isDown = false;

    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    } catch (Exception e) {
      log.error("HealthCheck: Database is down.", e);
      dbStatus = "ERROR";
      overallStatus = "DOWN";
      isDown = true;
    }

    try {
      s3Client.listBuckets();
    } catch (Exception e) {
      log.error("HealthCheck: S3 is down.", e);
      s3Status = "ERROR";
      overallStatus = "DOWN";
      isDown = true;
    }

    Runtime runtime = Runtime.getRuntime();
    long freeMemory = runtime.freeMemory();
    long totalMemory = runtime.totalMemory();

    if ((double) freeMemory / totalMemory < 0.10) {
      log.warn("HealthCheck: Low memory detected.");
      memoryStatus = "LOW";
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
