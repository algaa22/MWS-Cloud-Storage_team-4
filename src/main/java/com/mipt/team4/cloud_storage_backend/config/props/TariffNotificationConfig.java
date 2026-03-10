package com.mipt.team4.cloud_storage_backend.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.tariff.notification")
public class TariffNotificationConfig {

    private List<Integer> daysBeforeExpiry = List.of(7, 3, 1);
    private String notificationTime = "04:00";
    private boolean enabled = true;
}