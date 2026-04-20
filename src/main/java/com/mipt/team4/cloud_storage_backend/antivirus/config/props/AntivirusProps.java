package com.mipt.team4.cloud_storage_backend.antivirus.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "antivirus")
public record AntivirusProps(boolean enabled, Rabbitmq rabbitmq) {
  public record Rabbitmq(RoutingKeys routingKeys, Exchanges exchanges, Queues queues) {
    public record RoutingKeys(String tasks, String results) {}

    public record Exchanges(String tasks, String results) {}

    public record Queues(String results) {}
  }
}
