package com.mipt.team4.cloud_storage_backend.service.infrastructure;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MemoryHealthIndicator implements HealthIndicator {
  @Override
  public Health health() {
    double freePercent = countFreeMemoryPercent();

    Health.Builder status = freePercent > 0.1 ? Health.up() : Health.down();

    return status
        .withDetail("free_memory_percent", String.format("%.2f%%", freePercent * 100))
        .build();
  }

  private double countFreeMemoryPercent() {
    Runtime r = Runtime.getRuntime();
    return (double) (r.maxMemory() - (r.totalMemory() - r.freeMemory())) / r.maxMemory();
  }
}
