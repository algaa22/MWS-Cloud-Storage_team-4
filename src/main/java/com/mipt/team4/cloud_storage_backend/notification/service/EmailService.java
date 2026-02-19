package com.mipt.team4.cloud_storage_backend.notification.service;

import com.mipt.team4.cloud_storage_backend.notification.config.MailConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final MailConfig mailConfig;

  @Async
  public void sendHtmlEmail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(mailConfig.getUsername());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true); // true = это HTML

      mailSender.send(message);
      log.info("HTML Email sent to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send HTML email to: {}", to, e);
    }
  }

  // Старый метод для обратной совместимости
  @Async
  public void sendEmail(String to, String subject, String text) {
    sendHtmlEmail(to, subject, "<pre>" + text + "</pre>");
  }
}