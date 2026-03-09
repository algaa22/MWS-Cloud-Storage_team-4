package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
  private String type;
  private String userEmail;
  private String userName;
  private String fileName;
  private Long usedStorage;
  private Long storageLimit;
  private UUID userId;
  private String tariffName;
  private Integer daysLeft;
  private String endDate;
}
