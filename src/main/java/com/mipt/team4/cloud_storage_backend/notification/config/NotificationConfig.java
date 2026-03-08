package com.mipt.team4.cloud_storage_backend.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationConfig {

  private Subjects subjects = new Subjects();
  private Urls urls = new Urls();

  @Data
  public static class Subjects {
    private String fileDeleted = "📁 Файл был удален";
    private String storageAlmostFull = "⚠️ Внимание: заканчивается место в хранилище";
    private String storageFull = "❌ Хранилище полностью заполнено";
  }

  @Data
  public static class Urls {
    private String website = "http://localhost:5173";
    private String telegramSupport = "https://t.me/alg_aaa";
  }
}
