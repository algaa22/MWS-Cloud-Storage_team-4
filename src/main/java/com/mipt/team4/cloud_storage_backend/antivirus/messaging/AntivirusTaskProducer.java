package com.mipt.team4.cloud_storage_backend.antivirus.messaging;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanTaskDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AntivirusTaskProducer {
  private final RabbitTemplate rabbitTemplate;
  private final AntivirusProps antivirusProps;

  public void sendTask(ScanTaskDto task) {
    String exchange = antivirusProps.rabbitmq().exchanges().tasks();
    String routingKey = antivirusProps.rabbitmq().routingKeys().tasks();

    rabbitTemplate.convertAndSend(exchange, routingKey, task);
  }
}
