package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.props.NettyConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig.FailsafeRetry;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.netty.channel.MainChannelInitializer;
import com.mipt.team4.cloud_storage_backend.netty.handlers.common.ProtocolNegotiationHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager.ServerProtocol;
import com.mipt.team4.cloud_storage_backend.netty.ssl.SslContextFactory;
import com.mipt.team4.cloud_storage_backend.netty.utils.PipelineBuilder;
import dev.failsafe.RetryPolicy;
import java.net.URI;
import java.security.SecureRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ApplicationConfig {
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
  public S3Client s3Client(StorageConfig config) {
    StorageConfig.S3 s3Props = config.s3();

    AwsBasicCredentials credentials =
        AwsBasicCredentials.create(s3Props.accessKey(), s3Props.secretKey());

    return S3Client.builder()
        .endpointOverride(URI.create(s3Props.url()))
        .region(Region.of(s3Props.region()))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .forcePathStyle(true)
        .build();
  }

  @Bean
  public RetryPolicy<Object> retryPolicy(StorageConfig config) {
    FailsafeRetry retry = config.failsafeRetry();

    return RetryPolicy.builder()
        .handle(RecoverableStorageException.class)
        .withBackoff(retry.firstDelay(), retry.maxDelay(), retry.delayFactor())
        .withMaxRetries(retry.maxAttempts())
        .withJitter(retry.jitter())
        .build();
  }
}
