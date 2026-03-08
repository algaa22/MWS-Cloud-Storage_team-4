package com.mipt.team4.cloud_storage_backend.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class MailConfig {
  private String host;
  private int port;
  private String username;
  private String password;
  private String protocol;
  private boolean auth;
  private boolean starttls;
}
