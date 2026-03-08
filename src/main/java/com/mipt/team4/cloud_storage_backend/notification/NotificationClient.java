package com.mipt.team4.cloud_storage_backend.notification;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import java.util.UUID;

@Slf4j
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(@Value("${notification.service.url:http://localhost:8082}") String url) {
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .build();
    }

    public void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId) {
        NotificationRequest request = NotificationRequest.builder()
                .type("FILE_DELETED")
                .userEmail(userEmail)
                .userName(userName)
                .fileName(fileName)
                .userId(userId)
                .build();

        sendRequest(request);
    }

    public void notifyStorageAlmostFull(String userEmail, String userName,
                                        long usedStorage, long storageLimit, UUID userId) {
        NotificationRequest request = NotificationRequest.builder()
                .type("STORAGE_ALMOST_FULL")
                .userEmail(userEmail)
                .userName(userName)
                .usedStorage(usedStorage)
                .storageLimit(storageLimit)
                .userId(userId)
                .build();

        sendRequest(request);
    }

    public void notifyStorageFull(String userEmail, String userName, UUID userId) {
        NotificationRequest request = NotificationRequest.builder()
                .type("STORAGE_FULL")
                .userEmail(userEmail)
                .userName(userName)
                .userId(userId)
                .build();

        sendRequest(request);
    }

    private void sendRequest(NotificationRequest request) {
        try {
            webClient.post()
                    .uri("/api/notifications/send")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Notification sent successfully: {}", request.getType());
        } catch (WebClientException e) {
            log.error("Failed to send notification to {}: {}",
                    request.getType(), request.getUserEmail(), e);
        }
    }
}