package com.mipt.team4.cloud_storage_backend.model.user.entity;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserEntity {
  private final UUID id;
  boolean isActive;
  private String name;
  private String email;
  private String passwordHash;
  private long storageLimit;
  private long usedStorage;
  private LocalDateTime createdAt;

  public UserEntity(UUID id, String name, String email, String password) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = password;
    this.storageLimit = StorageConfig.INSTANCE.getDefaultStorageLimit();
    this.usedStorage = 0L;
    this.createdAt = LocalDateTime.now();
    this.isActive = true;
  }

  public UserEntity(
      UUID id,
      String name,
      String email,
      String password,
      long storageLimit,
      long usedStorage,
      LocalDateTime createdAt,
      boolean isActive
  ) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.passwordHash = password;
    this.storageLimit = storageLimit;
    this.usedStorage = usedStorage;
    this.createdAt = createdAt;
    this.isActive = isActive;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public long getStorageLimit() {
    return storageLimit;
  }

  public void setStorageLimit(long storageLimit) {
    this.storageLimit = storageLimit;
  }

  public long getUsedStorage() {
    return usedStorage;
  }

  public void setUsedStorage(long usedStorage) {
    this.usedStorage = usedStorage;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

}
