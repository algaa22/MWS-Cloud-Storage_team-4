package com.mipt.team4.cloud_storage_backend.antivirus.messaging;

import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanResultDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AntivirusResultConsumer {
  @RabbitListener(queues = "${antivirus.rabbitmq.queues.tasks}")
  public void handleScanResult(ScanResultDto result) {}
}
