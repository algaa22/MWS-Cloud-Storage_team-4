package com.mipt.team4.cloud_storage_backend.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mipt.team4.cloud_storage_backend.config.props.JacksonProps;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps.FailsafeRetry;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import dev.failsafe.RetryPolicy;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

  private final JacksonProps jacksonConfig;

  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }

  @Bean
  public RetryPolicy<Object> retryPolicy(StorageProps config) {
    FailsafeRetry retry = config.failsafeRetry();

    return RetryPolicy.builder()
        .handle(RecoverableStorageException.class)
        .withBackoff(retry.firstDelay(), retry.maxDelay(), retry.delayFactor())
        .withMaxRetries(retry.maxAttempts())
        .withJitter(retry.jitter())
        .build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper =
        new ObjectMapper()
            .deactivateDefaultTyping()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    JsonFactory factory = mapper.getFactory();
    factory.setStreamReadConstraints(
        StreamReadConstraints.builder()
            .maxNestingDepth(jacksonConfig.maxNestingLength())
            .maxStringLength(jacksonConfig.maxStringLength())
            .maxNumberLength(jacksonConfig.maxNumberLength())
            .build());

    return mapper;
  }
}
