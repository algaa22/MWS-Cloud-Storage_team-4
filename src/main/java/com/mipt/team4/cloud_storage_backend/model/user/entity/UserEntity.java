package com.mipt.team4.cloud_storage_backend.model.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {
  @EqualsAndHashCode.Include private final UUID id;
  @Builder.Default boolean isActive = true;
  private String name;
  private String email;
  private String passwordHash;
  private long storageLimit;
  @Builder.Default private long usedStorage = 0L;
  private LocalDateTime createdAt;
}
