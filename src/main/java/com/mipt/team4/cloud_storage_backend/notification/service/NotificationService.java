package com.mipt.team4.cloud_storage_backend.notification.service;

import com.mipt.team4.cloud_storage_backend.notification.model.Notification;
import com.mipt.team4.cloud_storage_backend.notification.model.NotificationType;
import com.mipt.team4.cloud_storage_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  //private final EmailService emailService;
  private final NotificationRepository notificationRepository;

  private static final String WEBSITE_URL = "http://localhost:3000"; // URL вашего React-приложения
  private static final String LOGO_URL = "https://i.imgur.com/YourLogo.png"; // Замените на свой логотип

  public void notifyFileDeleted(String userEmail, String userName, String filePath, UUID userId) {
    String subject = "📁 Файл был удален";

    String htmlContent = String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { padding: 30px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .content h2 { color: #333; margin-top: 0; }
                .info-box { background-color: #f8f9fa; border-left: 4px solid #28a745; padding: 20px; margin: 20px 0; border-radius: 5px; }
                .info-box p { margin: 0; color: #555; font-size: 16px; }
                .info-box strong { color: #007bff; }
                .button { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 20px 0; }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>☁️ Cloud Storage</h1>
                </div>
                <div class="content">
                    <h2>Здравствуйте, %s!</h2>
                    
                    <div class="info-box">
                        <p>Ваш файл <strong>'%s'</strong> был успешно удален из облачного хранилища.</p>
                    </div>
                    
                    <p style="color: #666; line-height: 1.6;">
                        Это автоматическое уведомление. Если вы не удаляли этот файл, 
                        <a href="%s/support" style="color: #667eea;">свяжитесь с поддержкой</a>.
                    </p>
                    
                    <div style="text-align: center;">
                        <a href="%s/files" class="button">📂 Перейти к файлам</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда Cloud Storage</p>
                        <p>© 2024 Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, userName, filePath, WEBSITE_URL, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, NotificationType.FILE_DELETED, userId, userName);
  }

  public void notifyStorageAlmostFull(String userEmail, String userName,
      long usedStorage, long storageLimit, UUID userId) {
    String subject = "⚠️ Внимание: заканчивается место в хранилище";
    double percentUsed = (usedStorage * 100.0) / storageLimit;
    int percentInt = (int) Math.round(percentUsed);

    String usedFormatted = formatBytes(usedStorage);
    String limitFormatted = formatBytes(storageLimit);

    String htmlContent = String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { padding: 30px 40px; background: linear-gradient(135deg, #ff6b6b 0%%, #feca57 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .stats { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                .stats table { width: 100%%; }
                .stats td { text-align: center; padding: 10px; }
                .stats-number { font-size: 28px; font-weight: bold; color: #dc3545; }
                .stats-label { color: #666; }
                .progress-bar { background-color: #f0f0f0; border-radius: 10px; height: 20px; overflow: hidden; margin: 15px 0; }
                .progress-fill { height: 100%%; background: linear-gradient(90deg, #ffc107, #dc3545); width: %d%%; border-radius: 10px; }
                .tips { margin: 20px 0; padding: 0; list-style: none; }
                .tips li { padding: 10px; background-color: #f8f9fa; margin: 5px 0; border-radius: 5px; }
                .button { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 20px 0; }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>⚠️ Внимание!</h1>
                </div>
                <div class="content">
                    <h2>Здравствуйте, %s!</h2>
                    
                    <p style="color: #666; font-size: 16px;">Ваше облачное хранилище почти заполнено:</p>
                    
                    <div class="stats">
                        <table>
                            <tr>
                                <td><div class="stats-number">%.1f%%</div><div class="stats-label">заполнено</div></td>
                                <td><div class="stats-number">%s</div><div class="stats-label">использовано</div></td>
                                <td><div class="stats-number">%s</div><div class="stats-label">всего</div></td>
                            </tr>
                        </table>
                        
                        <div class="progress-bar">
                            <div class="progress-fill" style="width: %d%%;"></div>
                        </div>
                    </div>
                    
                    <p style="color: #666;">Рекомендуем:</p>
                    
                    <ul class="tips">
                        <li>🧹 <strong>Очистить хранилище</strong> — удалите ненужные файлы</li>
                        <li>📦 <strong>Архивировать старые файлы</strong> — сожмите редко используемые данные</li>
                        <li>⭐ <strong>Увеличить лимит</strong> — перейдите на премиум-тариф</li>
                    </ul>
                    
                    <div style="text-align: center;">
                        <a href="%s/storage" class="button">📊 Управлять хранилищем</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда Cloud Storage</p>
                        <p>© 2026 Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, percentInt, userName, percentUsed, usedFormatted, limitFormatted, percentInt, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, NotificationType.STORAGE_ALMOST_FULL, userId, userName);
  }

  public void notifyStorageFull(String userEmail, String userName, UUID userId) {
    String subject = "❌ Хранилище полностью заполнено";

    String htmlContent = String.format("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                .header { padding: 30px 40px; background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .alert-box { background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 20px; border-radius: 8px; margin: 20px 0; }
                .actions { margin: 20px 0; }
                .action-item { padding: 10px; background-color: #f8f9fa; border-radius: 5px; margin: 5px 0; }
                .button-primary { background: #dc3545; color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin-right: 10px; }
                .button-secondary { background: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>❌ Хранилище заполнено</h1>
                </div>
                <div class="content">
                    <h2>Здравствуйте, %s!</h2>
                    
                    <div class="alert-box">
                        <p style="margin: 0; font-size: 16px;">
                            ⚠️ Ваше облачное хранилище полностью заполнено. Вы не сможете загружать новые файлы, 
                            пока не освободите место.
                        </p>
                    </div>
                    
                    <p style="color: #666;"><strong>Что можно сделать прямо сейчас:</strong></p>
                    
                    <div class="actions">
                        <div class="action-item">🗑️ <strong>Удалить ненужные файлы</strong> — очистите корзину</div>
                        <div class="action-item">📦 <strong>Архивировать старые проекты</strong> — сожмите их в ZIP</div>
                        <div class="action-item">⭐ <strong>Обновить тариф</strong> — получите больше места</div>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s/storage?action=clean" class="button-primary">🧹 Очистить</a>
                        <a href="%s/pricing" class="button-secondary">⭐ Обновить тариф</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда Cloud Storage</p>
                        <p>© 2024 Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, userName, WEBSITE_URL, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, NotificationType.STORAGE_FULL, userId, userName);
  }

  // Метод для отправки с HTML
  private void sendNotification(String userEmail, String subject,
      String htmlContent, NotificationType type,
      UUID userId, String userName) {
    // Отправляем HTML email
    //emailService.sendHtmlEmail(userEmail, subject, htmlContent);

    // Для БД сохраняем чистый текст (без HTML)
    String plainText = htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();

    Notification notification = Notification.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .userEmail(userEmail)
        .type(type)
        .subject(subject)
        .content(plainText)
        .createdAt(LocalDateTime.now())
        .isRead(false)
        .build();

    notificationRepository.save(notification);
    log.info("HTML Notification sent to {}: {}", userEmail, subject);
  }

  // Форматирование байтов в читаемый вид
  private String formatBytes(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp-1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }
}