package com.mipt.team4.cloud_storage_backend.notification.controller;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.notification.model.Notification;
import com.mipt.team4.cloud_storage_backend.notification.repository.NotificationRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationRepository notificationRepository;
  private final UserSessionService userSessionService;

  @GetMapping
  public List<Notification> getMyNotifications(@RequestHeader("Authorization") String token)
      throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(token);
    return notificationRepository.findByUserId(userId);
  }

  @GetMapping("/unread/count")
  public int getUnreadCount(@RequestHeader("Authorization") String token)
      throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(token);
    return notificationRepository.getUnreadCount(userId);
  }

  @PostMapping("/{id}/read")
  public void markAsRead(@PathVariable UUID id) {
    notificationRepository.markAsRead(id);
  }

  @PostMapping("/read-all")
  public void markAllAsRead(@RequestHeader("Authorization") String token)
      throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(token);
    List<Notification> notifications = notificationRepository.findByUserId(userId);
    notifications.stream()
        .filter(n -> !n.isRead())
        .forEach(n -> notificationRepository.markAsRead(n.getId()));
  }
}