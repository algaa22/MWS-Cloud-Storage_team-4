package com.mipt.team4.cloud_storage_backend.config.props;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProps(
    Rest rest,
    Auth auth,
    Quotas quotas,
    StateMachine stateMachine,
    FailsafeRetry failsafeRetry,
    Trash trash,
    S3 s3,
    HealthCheck healthCheck,
    Scheduling scheduling) {
  public record Rest(int maxAggregatedContentLength, int fileDownloadChunkSize) {}

  public record Auth(
      String jwtSecretKey, long accessTokenExpirationSec, long refreshTokenExpirationSec) {}

  public record Quotas(long defaultStorageLimit) {}

  public record StateMachine(int maxRetryCount, int fileThrottledUpdateIntervalSec) {}

  public record FailsafeRetry(
      int maxAttempts, double delayFactor, Duration firstDelay, Duration maxDelay, double jitter) {}

  public record Trash(int retentionDays) {}

  public record S3(
      long minFilePartSize,
      String url,
      String accessKey,
      String secretKey,
      String region,
      UserDataBucket userDataBucket,
      Limits limits) {
    public record UserDataBucket(String name) {}

    public record Limits(long minFilePartSize, long maxFilePartSize, int maxPartsNum) {}
  }

  public record HealthCheck(
      long intervalSeconds,
      int dbTimeoutSeconds,
      int s3TimeoutSeconds,
      double minFreeMemoryPercent,
      long checkIntervalSeconds) {}

  public record Scheduling(
      int pageSize, int dangerousDeletionTimeDays, StaleTimeMin staleTimeMin, Cron cron) {
    public record StaleTimeMin(int file, int upload, int scan) {}

    public record Cron(
        String infectedFileCleanup,
        String staleFileCleanup,
        String staleUploadCleanup,
        String staleScan,
        String trashCleanup,
        String checkTariff,
        String quotaSync) {}
  }
}
