package com.mipt.team4.cloud_storage_backend.service.infrastructure;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig.HealthCheck;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.HealthCheckResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthComponentStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthMemoryStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthOverallStatus;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthMonitorService {

  private final JdbcTemplate jdbcTemplate;
  private final S3Client s3Client;
  private final StorageConfig storageConfig;

  private final AtomicReference<HealthCheckResponse> currentHealth =
      new AtomicReference<>(
          new HealthCheckResponse(
              HttpResponseStatus.OK,
              HealthOverallStatus.STARTING,
              HealthComponentStatus.UNKNOWN,
              HealthComponentStatus.UNKNOWN,
              HealthMemoryStatus.UNKNOWN,
              Instant.now().toString()));

  private ScheduledExecutorService scheduler;

  @PostConstruct
  public void startMonitoring() {
    final long checkIntervalSeconds = storageConfig.healthCheck().checkIntervalSeconds();

    scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    scheduler.scheduleWithFixedDelay(
        this::performChecks, 0, checkIntervalSeconds, TimeUnit.SECONDS);
    log.info(
        "Health monitor started with interval {} seconds on Virtual Thread.", checkIntervalSeconds);
  }

  @PreDestroy
  public void stopMonitoring() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
      log.info("Health monitor stopped.");
    }
  }

  public HealthCheckResponse getCurrentHealth() {
    return currentHealth.get();
  }

  private void performChecks() {
    StorageConfig.HealthCheck config = storageConfig.healthCheck();

    HealthComponentStatus dbStatus = HealthComponentStatus.OK;
    HealthComponentStatus s3Status = HealthComponentStatus.OK;
    HealthMemoryStatus memoryStatus = HealthMemoryStatus.OK;
    HealthOverallStatus overallStatus = HealthOverallStatus.UP;

    try {
      checkDatabase(config);
    } catch (Exception e) {
      log.error("Background HealthCheck: Database is down: {}", e.getMessage());
      dbStatus = HealthComponentStatus.ERROR;
      overallStatus = HealthOverallStatus.DOWN;
    }

    try {
      checkS3(config);
    } catch (Exception e) {
      log.error("Background HealthCheck: S3 is down: {}", e.getMessage());
      s3Status = HealthComponentStatus.ERROR;
      overallStatus = HealthOverallStatus.DOWN;
    }

    if (countMemoryUsagePercent() < storageConfig.healthCheck().minFreeMemoryPercent()) {
      log.warn("Background HealthCheck: Low memory detected.");
      memoryStatus = HealthMemoryStatus.LOW;
    }

    HttpResponseStatus httpStatus =
        overallStatus == HealthOverallStatus.DOWN
            ? HttpResponseStatus.SERVICE_UNAVAILABLE
            : HttpResponseStatus.OK;
    String timestamp = Instant.now().toString();

    HealthCheckResponse newStatus =
        new HealthCheckResponse(
            httpStatus, overallStatus, dbStatus, s3Status, memoryStatus, timestamp);
    currentHealth.set(newStatus);
  }

  private void checkDatabase(HealthCheck config) {
    jdbcTemplate.execute(
        (StatementCallback<Boolean>)
            stmt -> {
              stmt.setQueryTimeout(config.dbTimeoutSeconds());
              return stmt.execute("SELECT 1");
            });
  }

  private void checkS3(HealthCheck config) {
    s3Client.listBuckets(
        ListBucketsRequest.builder()
            .overrideConfiguration(
                conf ->
                    conf.apiCallTimeout(Duration.ofSeconds(config.s3TimeoutSeconds()))
                        .apiCallAttemptTimeout(Duration.ofSeconds(config.s3TimeoutSeconds())))
            .build());
  }

  private double countMemoryUsagePercent() {
    Runtime runtime = Runtime.getRuntime();

    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeAllocatedMemory = runtime.freeMemory();

    long usedMemory = allocatedMemory - freeAllocatedMemory;
    long actualFreeMemory = maxMemory - usedMemory;

    return (double) actualFreeMemory / maxMemory;
  }
}
