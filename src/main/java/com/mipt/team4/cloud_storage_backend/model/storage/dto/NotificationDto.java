package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
  private String type;
  private String userEmail;
  private String userName;
  private UUID userId;
  private String fileName;
  private Long usedStorage;
  private Long storageLimit;
  private String tariffName;
  private String endDate;
  private String deletionDate;
  private Integer daysLeft;
}
