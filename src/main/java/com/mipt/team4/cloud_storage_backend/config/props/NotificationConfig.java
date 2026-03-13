package com.mipt.team4.cloud_storage_backend.config.props;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationConfig(
    double fullThreshold, double almostFullThreshold, Tariff tariff, Service service) {
  public record Tariff(List<Integer> daysBeforeExpiry) {}

  public record Service(String url) {}
}
