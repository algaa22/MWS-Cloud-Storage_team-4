package com.mipt.team4.cloud_storage_backend.notification;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.NotificationDto;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@Profile("!test")
@Primary
public class WebNotificationClient implements NotificationClient {
  private final WebClient webClient;

  public WebNotificationClient(
      @Value("${notification.service.url:http://localhost:8082}") String url) {
    this.webClient = WebClient.builder().baseUrl(url).build();
  }

  @Override
  public void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId) {
    NotificationDto request =
        NotificationDto.builder()
            .type("FILE_DELETED")
            .userEmail(userEmail)
            .userName(userName)
            .fileName(fileName)
            .userId(userId)
            .build();

    sendRequest(request);
  }

  @Override
  public void notifyStorageAlmostFull(
      String userEmail, String userName, long usedStorage, long storageLimit, UUID userId) {
    NotificationDto request =
        NotificationDto.builder()
            .type("STORAGE_ALMOST_FULL")
            .userEmail(userEmail)
            .userName(userName)
            .usedStorage(usedStorage)
            .storageLimit(storageLimit)
            .userId(userId)
            .build();

    sendRequest(request);
  }

  @Override
  public void notifyStorageFull(String userEmail, String userName, UUID userId) {
    NotificationDto request =
        NotificationDto.builder()
            .type("STORAGE_FULL")
            .userEmail(userEmail)
            .userName(userName)
            .userId(userId)
            .build();

    sendRequest(request);
  }

  @Override
  public void notifyTariffPurchased(
      String email, String name, String tariffName, LocalDateTime endDate) {
    NotificationDto request =
        NotificationDto.builder()
            .type("TARIFF_PURCHASED")
            .userEmail(email)
            .userName(name)
            .tariffName(tariffName)
            .endDate(endDate != null ? endDate.toString() : null)
            .build();
    sendRequest(request);
  }

  @Override
  public void notifyTariffEndingSoon(
      String email, String name, int daysLeft, LocalDateTime endDate) {
    NotificationDto request =
        NotificationDto.builder()
            .type("TARIFF_ENDING_SOON")
            .userEmail(email)
            .userName(name)
            .daysLeft(daysLeft)
            .endDate(endDate != null ? endDate.toString() : null)
            .build();
    sendRequest(request);
  }

  @Override
  public void notifyTariffExpired(String email, String name) {
    NotificationDto request =
        NotificationDto.builder().type("TARIFF_EXPIRED").userEmail(email).userName(name).build();
    sendRequest(request);
  }

  @Override
  public void notifyTariffRenewed(
      String email, String name, String tariffName, LocalDateTime newEndDate) {
    NotificationDto request =
        NotificationDto.builder()
            .type("TARIFF_RENEWED")
            .userEmail(email)
            .userName(name)
            .tariffName(tariffName)
            .endDate(newEndDate != null ? newEndDate.toString() : null)
            .build();
    sendRequest(request);
  }

  @Override
  public void notifySubscriptionExpired(String email, String name, LocalDateTime deletionDate) {
    NotificationDto request =
        NotificationDto.builder()
            .type("SUBSCRIPTION_EXPIRED")
            .userEmail(email)
            .userName(name)
            .deletionDate(deletionDate != null ? deletionDate.toString() : null)
            .build();
    sendRequest(request);
  }

  @Override
  public void notifyFilesDeleted(String email, String name) {
    NotificationDto request =
        NotificationDto.builder().type("FILES_DELETED").userEmail(email).userName(name).build();
    sendRequest(request);
  }

  @Override
  public void notifyTrialExpired(String email, String name) {
    NotificationDto request =
        NotificationDto.builder().type("TRIAL_EXPIRED").userEmail(email).userName(name).build();
    sendRequest(request);
  }

  @Override
  public void notifyTrialStarted(String email, String name, LocalDateTime trialEndDate) {
    NotificationDto request =
        NotificationDto.builder()
            .type("TRIAL_STARTED")
            .userEmail(email)
            .userName(name)
            .endDate(trialEndDate != null ? trialEndDate.toString() : null)
            .build();
    sendRequest(request);
  }

  public void notifyDangerousFile(
      String userEmail,
      String userName,
      String fileName,
      String folderPath,
      String verdict,
      UUID userId) {

    NotificationDto request =
        NotificationDto.builder()
            .type("FILE_INFECTED")
            .userEmail(userEmail)
            .userName(userName)
            .fileName(fileName)
            .folderPath(folderPath)
            .verdict(verdict)
            .userId(userId)
            .build();

    sendRequest(request);
  }

  public void notifyScanError(
      String userEmail, String userName, String fileName, String folderPath, UUID userId) {

    NotificationDto request =
        NotificationDto.builder()
            .type("SCAN_ERROR")
            .userEmail(userEmail)
            .userName(userName)
            .fileName(fileName)
            .folderPath(folderPath)
            .userId(userId)
            .build();

    sendRequest(request);
  }

  private void sendRequest(NotificationDto request) {
    webClient
        .post()
        .uri("/api/notifications/send")
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .subscribe(
            response -> log.debug("Notification sent successfully to {}", request.getUserEmail()),
            error ->
                log.error(
                    "Failed to send notification to {}: {}. Error: {}",
                    request.getUserId(),
                    request.getUserEmail(),
                    error.getMessage()));
  }
}
