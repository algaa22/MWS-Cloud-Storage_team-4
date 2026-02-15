package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "database")
public record DatabaseConfig(String url, String username, String password) {}
