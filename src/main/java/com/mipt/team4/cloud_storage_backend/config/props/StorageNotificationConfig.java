package com.mipt.team4.cloud_storage_backend.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage.notification")
public class StorageNotificationConfig {
  private double fullThreshold = 0.95;
  private double almostFullThreshold = 0.75;
}
