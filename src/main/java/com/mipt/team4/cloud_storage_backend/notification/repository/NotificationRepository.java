package com.mipt.team4.cloud_storage_backend.notification.repository;

import com.mipt.team4.cloud_storage_backend.notification.model.Notification;
import com.mipt.team4.cloud_storage_backend.notification.model.NotificationType;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

  private final PostgresConnection postgres;

  public void save(Notification notification) {
    postgres.executeUpdate(
        "INSERT INTO notifications (id, user_id, user_email, type, subject, content, created_at, is_read) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",  // ← добавили user_id
        List.of(
            notification.getId(),
            notification.getUserId(),      // ← новое поле
            notification.getUserEmail(),
            notification.getType().name(),
            notification.getSubject(),
            notification.getContent(),
            notification.getCreatedAt(),
            notification.isRead()
        )
    );
  }

  // Новый метод - получить уведомления по ID пользователя (для сайта)
  public List<Notification> findByUserId(UUID userId) {
    return postgres.executeQuery(
        "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC",
        List.of(userId),
        this::mapToNotification
    );
  }

  // Старый метод (оставить для совместимости)
  public List<Notification> findByUserEmail(String userEmail) {
    return postgres.executeQuery(
        "SELECT * FROM notifications WHERE user_email = ? ORDER BY created_at DESC",
        List.of(userEmail),
        this::mapToNotification
    );
  }

  public void markAsRead(UUID notificationId) {
    postgres.executeUpdate(
        "UPDATE notifications SET is_read = true WHERE id = ?",
        List.of(notificationId)
    );
  }

  // Получить количество непрочитанных для пользователя
  public int getUnreadCount(UUID userId) {
    List<Integer> result = postgres.executeQuery(
        "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false",
        List.of(userId),
        rs -> rs.getInt(1)
    );
    return result.isEmpty() ? 0 : result.get(0);
  }

  private Notification mapToNotification(ResultSet rs) throws SQLException {
    return Notification.builder()
        .id(UUID.fromString(rs.getString("id")))
        .userId(UUID.fromString(rs.getString("user_id")))  // ← новое поле
        .userEmail(rs.getString("user_email"))
        .type(NotificationType.valueOf(rs.getString("type")))
        .subject(rs.getString("subject"))
        .content(rs.getString("content"))
        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
        .isRead(rs.getBoolean("is_read"))
        .build();
  }
}