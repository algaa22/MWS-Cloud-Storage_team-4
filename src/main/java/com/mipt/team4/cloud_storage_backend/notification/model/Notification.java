package com.mipt.team4.cloud_storage_backend.notification.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Notification {
  private UUID id;
  private UUID userId;           // ← ДОБАВИТЬ (для связи с пользователем)
  private String userEmail;       // оставить для истории
  private NotificationType type;
  private String subject;
  private String content;
  private LocalDateTime createdAt;
  private boolean isRead;
}