package com.mipt.team4.cloud_storage_backend.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mipt.team4.cloud_storage_backend.config.props.JacksonConfig;
import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps.FailsafeRetry;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.netty.channel.MainChannelInitializer;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import dev.failsafe.RetryPolicy;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.AttributeMap;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {
  private final JacksonConfig jacksonConfig;

  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }

  @Bean
  public MainChannelInitializer httpChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyConfig nettyConfig) {
    return new MainChannelInitializer(
        pipelineBuilder,
        sslContextFactory,
        protocolNegotiationHandler,
        nettyConfig,
        ServerProtocol.HTTP);
  }

  @Bean
  public MainChannelInitializer httpsChannelInitializer(
      PipelineBuilder pipelineBuilder,
      SslContextFactory sslContextFactory,
      ObjectProvider<ProtocolNegotiationHandler> protocolNegotiationHandler,
      NettyConfig nettyConfig) {
    return new MainChannelInitializer(
        pipelineBuilder,
        sslContextFactory,
        protocolNegotiationHandler,
        nettyConfig,
        ServerProtocol.HTTPS);
  }

  @Bean
  public S3Client s3Client(StorageProps config) {
    StorageProps.S3 s3Props = config.s3();

    return S3Client.builder()
        .endpointOverride(URI.create(s3Props.url()))
        .region(Region.of(s3Props.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Props.accessKey(), s3Props.secretKey())))
        .forcePathStyle(true)
        .httpClient(
            UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(s3Props.timeoutsSec().connection()))
                .socketTimeout(Duration.ofSeconds(s3Props.timeoutsSec().socket()))
                .buildWithDefaults(
                    AttributeMap.builder()
                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                        .build()))
        .build();
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
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.deactivateDefaultTyping();

    JsonFactory factory = mapper.getFactory();
    factory.setStreamReadConstraints(
        StreamReadConstraints.builder()
            .maxNestingDepth(jacksonConfig.maxNestingLength())
            .maxStringLength(jacksonConfig.maxStringLength())
            .maxNumberLength(jacksonConfig.maxNumberLength())
            .build());

    return mapper;
  }

  @Bean
  public S3Presigner s3Presigner(StorageProps config) {
    StorageProps.S3 s3Props = config.s3();

    return S3Presigner.builder()
        .endpointOverride(URI.create(s3Props.url()))
        .region(Region.of(s3Props.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Props.accessKey(), s3Props.secretKey())))
        .serviceConfiguration(
            software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
        .build();
  }
}
