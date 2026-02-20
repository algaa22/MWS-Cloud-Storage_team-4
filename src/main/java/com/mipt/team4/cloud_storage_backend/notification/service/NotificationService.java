package com.mipt.team4.cloud_storage_backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final EmailService emailService;

  private static final String WEBSITE_URL = "http://localhost:5173";
  private static final String TELEGRAM_SUPPORT = "https://t.me/alg_aaa";

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
                .header { padding: 30px 40px; background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .content h2 { color: #333; margin-top: 0; }
                .info-box { background-color: #f8f9fa; border-left: 4px solid #3b82f6; padding: 20px; margin: 20px 0; border-radius: 5px; }
                .info-box p { margin: 0; color: #555; font-size: 16px; }
                .info-box strong { color: #1e40af; }
                .button { background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 20px 0; transition: transform 0.2s; }
                .button:hover { transform: scale(1.05); }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
                .support-link { color: #3b82f6; text-decoration: none; font-weight: 500; }
                .support-link:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>☁️ MWS Cloud Storage</h1>
                </div>
                <div class="content">
                    <h2>Здравствуйте, %s!</h2>
                    
                    <div class="info-box">
                        <p>Ваш файл <strong>'%s'</strong> был успешно удален из облачного хранилища.</p>
                    </div>
                    
                    <p style="color: #666; line-height: 1.6;">
                        Это автоматическое уведомление. Если вы не удаляли этот файл, 
                        <a href="%s" class="support-link" target="_blank">свяжитесь с поддержкой в Telegram</a>.
                    </p>
                    
                    <div style="text-align: center;">
                        <a href="%s/files" class="button" target="_blank">📂 Перейти к файлам</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда MWS Cloud Storage</p>
                        <p>© 2026 MWS Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, userName, filePath, TELEGRAM_SUPPORT, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, userId, userName);
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
                .header { padding: 30px 40px; background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .content h2 { color: #333; margin-top: 0; }
                .stats { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #f59e0b; }
                .stats table { width: 100%%; }
                .stats td { text-align: center; padding: 10px; }
                .stats-number { font-size: 28px; font-weight: bold; color: #d97706; }
                .stats-label { color: #666; }
                .progress-bar { background-color: #f0f0f0; border-radius: 10px; height: 20px; overflow: hidden; margin: 15px 0; }
                .progress-fill { height: 100%%; background: linear-gradient(90deg, #f59e0b, #d97706); width: %d%%; border-radius: 10px; }
                .tips { margin: 20px 0; padding: 0; list-style: none; }
                .tips li { padding: 10px; background-color: #f8f9fa; margin: 5px 0; border-radius: 5px; }
                .button { background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 20px 0; transition: transform 0.2s; }
                .button:hover { transform: scale(1.05); }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
                .support-link { color: #3b82f6; text-decoration: none; font-weight: 500; }
                .support-link:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>⚠️ MWS Cloud Storage</h1>
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
                    
                    <p style="color: #666;"><strong>Что можно сделать:</strong></p>
                    
                    <ul class="tips">
                        <li>🧹 <strong>Очистить хранилище</strong> — удалите ненужные файлы</li>
                        <li>📦 <strong>Архивировать старые файлы</strong> — сожмите редко используемые данные</li>
                        <li>⭐ <strong>Увеличить лимит</strong> — перейдите на премиум-тариф</li>
                    </ul>
                    
                    <p style="color: #666; line-height: 1.6;">
                        Нужна помощь? 
                        <a href="%s" class="support-link" target="_blank">Напишите в Telegram</a>
                    </p>
                    
                    <div style="text-align: center;">
                        <a href="%s/files" class="button" target="_blank">📊 Управлять хранилищем</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда MWS Cloud Storage</p>
                        <p>© 2026 MWS Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, percentInt, userName, percentUsed, usedFormatted, limitFormatted, percentInt, TELEGRAM_SUPPORT, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, userId, userName);
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
                .header { padding: 30px 40px; background: linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%); border-radius: 10px 10px 0 0; text-align: center; }
                .header h1 { color: white; margin: 0; font-size: 24px; }
                .content { padding: 40px; }
                .content h2 { color: #333; margin-top: 0; }
                .alert-box { background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 20px; margin: 20px 0; border-radius: 5px; }
                .alert-box p { margin: 0; color: #555; font-size: 16px; }
                .actions { margin: 20px 0; }
                .action-item { padding: 10px; background-color: #f8f9fa; margin: 5px 0; border-radius: 5px; border-left: 4px solid #ef4444; }
                .button { background: linear-gradient(135deg, #3b82f6 0%%, #1e40af 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 10px; transition: transform 0.2s; }
                .button:hover { transform: scale(1.05); }
                .button-secondary { background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 25px; font-weight: bold; display: inline-block; margin: 10px; transition: transform 0.2s; }
                .button-secondary:hover { transform: scale(1.05); }
                .footer { text-align: center; padding: 20px; color: #999; font-size: 14px; border-top: 1px solid #eee; }
                .support-link { color: #3b82f6; text-decoration: none; font-weight: 500; }
                .support-link:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>❌ MWS Cloud Storage</h1>
                </div>
                <div class="content">
                    <h2>Здравствуйте, %s!</h2>
                    
                    <div class="alert-box">
                        <p>⚠️ Ваше облачное хранилище полностью заполнено. Вы не сможете загружать новые файлы, пока не освободите место.</p>
                    </div>
                    
                    <p style="color: #666;"><strong>Что можно сделать прямо сейчас:</strong></p>
                    
                    <div class="actions">
                        <div class="action-item">🗑️ <strong>Удалить ненужные файлы</strong> — очистите корзину</div>
                        <div class="action-item">📦 <strong>Архивировать старые проекты</strong> — сожмите их в ZIP</div>
                        <div class="action-item">⭐ <strong>Обновить тариф</strong> — получите больше места</div>
                    </div>
                    
                    <p style="color: #666; line-height: 1.6;">
                        Нужна помощь? 
                        <a href="%s" class="support-link" target="_blank">Напишите в Telegram</a>
                    </p>
                    
                    <div style="text-align: center;">
                        <a href="%s/files" class="button" target="_blank">🧹 Очистить хранилище</a>
                        <a href="%s/settings" class="button-secondary" target="_blank">⭐ Обновить тариф</a>
                    </div>
                    
                    <div class="footer">
                        <p>С уважением, команда MWS Cloud Storage</p>
                        <p>© 2026 MWS Cloud Storage. Все права защищены.</p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """, userName, TELEGRAM_SUPPORT, WEBSITE_URL, WEBSITE_URL);

    sendNotification(userEmail, subject, htmlContent, userId, userName);
  }

  private void sendNotification(String userEmail, String subject,
      String htmlContent, UUID userId, String userName) {
    emailService.sendHtmlEmail(userEmail, subject, htmlContent);
    log.info("HTML Notification sent to {}: {}", userEmail, subject);
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp-1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }
}