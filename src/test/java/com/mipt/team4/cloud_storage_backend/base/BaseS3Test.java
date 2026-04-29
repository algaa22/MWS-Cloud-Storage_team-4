package com.mipt.team4.cloud_storage_backend.base;

import com.mipt.team4.cloud_storage_backend.base.BaseS3Test.S3TestConfig;
import com.mipt.team4.cloud_storage_backend.base.extensions.S3Extension;
import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import com.mipt.team4.cloud_storage_backend.repository.storage.S3ContentRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.S3Wrapper;
import java.net.URI;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ActiveProfiles("test")
@ExtendWith(S3Extension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {S3ContentRepository.class, S3Wrapper.class, S3TestConfig.class})
@EnableConfigurationProperties(StorageProps.class)
public abstract class BaseS3Test {

  @MockitoBean private NettyServerManager nettyServerManager;

  @TestConfiguration
  static class S3TestConfig {

    @Bean
    public S3Client s3Client(StorageProps config) {
      return S3Client.builder()
          .endpointOverride(URI.create(config.s3().url()))
          .region(Region.of(config.s3().region()))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(config.s3().accessKey(), config.s3().secretKey())))
          .requestChecksumCalculation(RequestChecksumCalculation.WHEN_SUPPORTED)
          .responseChecksumValidation(ResponseChecksumValidation.WHEN_SUPPORTED)
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
          .build();
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
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
          .build();
    }
  }
}
