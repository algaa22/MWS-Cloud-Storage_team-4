package com.mipt.team4.cloud_storage_backend.antivirus.config;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;

public class AntivirusRabbitConfig {
  @Bean
  public Exchange tasksExchange(AntivirusProps props) {
    return new DirectExchange(props.rabbitmq().exchanges().tasks());
  }

  @Bean
  public Exchange resultsExchange(AntivirusProps props) {
    return new DirectExchange(props.rabbitmq().exchanges().results());
  }

  @Bean
  public Queue resultsQueue(AntivirusProps props) {
    return QueueBuilder.durable(props.rabbitmq().queues().results()).build();
  }

  @Bean
  public Binding resultsBinding(
      Queue resultsQueue, Exchange resultsExchange, AntivirusProps props) {
    return BindingBuilder.bind(resultsQueue)
        .to(resultsExchange)
        .with(props.rabbitmq().routingKeys().results())
        .noargs();
  }
}
