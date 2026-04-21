package com.mipt.team4.cloud_storage_backend.antivirus.messaging;

import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanResultDto;
import com.mipt.team4.cloud_storage_backend.antivirus.service.AntivirusService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AntivirusResultConsumer {
  private final AntivirusService antivirusService;

  @RabbitListener(queues = "${antivirus.rabbitmq.queues.results}")
  public void handleScanResult(ScanResultDto result) {
    antivirusService.handleScanResult(result);
  }
}
