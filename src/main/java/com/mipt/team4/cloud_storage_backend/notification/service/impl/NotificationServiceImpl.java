package com.mipt.team4.cloud_storage_backend.notification.service.impl;

import com.mipt.team4.cloud_storage_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  @Override
  public CompletableFuture<Boolean> notifyFileDeleted(String userEmail, String fileName) {
    log.info("🔔 Файл удалён: '{}' у пользователя {}", fileName, userEmail);
    // TODO: здесь потом добавится отправка email
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Boolean> notifyStorageLimitWarning(String userEmail, long usedSpace, long totalSpace) {
    double usagePercent = (usedSpace * 100.0) / totalSpace;
    DecimalFormat df = new DecimalFormat("#.##");

    log.warn("⚠️ Хранилище пользователя {} заполнено на {}%", userEmail, df.format(usagePercent));
    // TODO: здесь потом добавится отправка email
    return CompletableFuture.completedFuture(true);
  }
}