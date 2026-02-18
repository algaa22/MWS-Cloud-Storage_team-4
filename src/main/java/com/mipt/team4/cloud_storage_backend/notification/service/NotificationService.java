package com.mipt.team4.cloud_storage_backend.notification.service;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {
  CompletableFuture<Boolean> notifyFileDeleted(String userEmail, String fileName);
  CompletableFuture<Boolean> notifyStorageLimitWarning(String userEmail, long usedSpace, long totalSpace);
}