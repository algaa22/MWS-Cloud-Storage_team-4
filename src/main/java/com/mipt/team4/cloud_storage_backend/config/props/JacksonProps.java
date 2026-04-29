package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jackson")
public record JacksonProps(int maxNestingLength, int maxStringLength, int maxNumberLength) {}
