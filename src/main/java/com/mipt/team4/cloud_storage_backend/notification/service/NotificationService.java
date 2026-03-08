package com.mipt.team4.cloud_storage_backend.notification.service;

import com.mipt.team4.cloud_storage_backend.notification.config.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.utils.FormatUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final EmailService emailService;
  private final NotificationConfig notificationConfig;
  private final SpringTemplateEngine templateEngine;

  public void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId) {
    String subject = notificationConfig.getSubjects().getFileDeleted();

    Context context = new Context();
    context.setVariable("userName", userName);
    context.setVariable("fileName", fileName);
    context.setVariable("websiteUrl", notificationConfig.getUrls().getWebsite());
    context.setVariable("telegramSupportUrl", notificationConfig.getUrls().getTelegramSupport());

    String htmlContent = templateEngine.process("email/file-deleted", context);
    sendNotification(userEmail, subject, htmlContent, userId, userName);
  }

  public void notifyStorageAlmostFull(
      String userEmail, String userName, long usedStorage, long storageLimit, UUID userId) {
    String subject = notificationConfig.getSubjects().getStorageAlmostFull();

    double percentUsed = (usedStorage * 100.0) / storageLimit;
    int percentInt = (int) Math.round(percentUsed);
    String percentFormatted = String.format("%.1f%%", percentUsed);
    String usedFormatted = FormatUtils.formatBytes(usedStorage);
    String limitFormatted = FormatUtils.formatBytes(storageLimit);

    Context context = new Context();
    context.setVariable("userName", userName);
    context.setVariable("usedFormatted", usedFormatted);
    context.setVariable("limitFormatted", limitFormatted);
    context.setVariable("percentInt", percentInt);
    context.setVariable("percentFormatted", percentFormatted);
    context.setVariable("websiteUrl", notificationConfig.getUrls().getWebsite());
    context.setVariable("telegramSupportUrl", notificationConfig.getUrls().getTelegramSupport());

    String htmlContent = templateEngine.process("email/storage-almost-full", context);
    sendNotification(userEmail, subject, htmlContent, userId, userName);
  }

  public void notifyStorageFull(String userEmail, String userName, UUID userId) {
    String subject = notificationConfig.getSubjects().getStorageFull();

    Context context = new Context();
    context.setVariable("userName", userName);
    context.setVariable("websiteUrl", notificationConfig.getUrls().getWebsite());
    context.setVariable("telegramSupportUrl", notificationConfig.getUrls().getTelegramSupport());

    String htmlContent = templateEngine.process("email/storage-full", context);
    sendNotification(userEmail, subject, htmlContent, userId, userName);
  }

  private void sendNotification(
      String userEmail, String subject, String htmlContent, UUID userId, String userName) {
    emailService.sendHtmlEmail(userEmail, subject, htmlContent);
    log.info("HTML Notification sent to {}: {}", userEmail, subject);
  }
}
