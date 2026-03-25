package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jackson")
public record JacksonConfig(int maxNestingLength, int maxStringLength, int maxNumberLength) {}
