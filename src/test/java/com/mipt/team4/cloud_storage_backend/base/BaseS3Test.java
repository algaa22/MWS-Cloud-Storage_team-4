package com.mipt.team4.cloud_storage_backend.base;

import com.mipt.team4.cloud_storage_backend.base.BaseS3Test.S3TestConfig;
import com.mipt.team4.cloud_storage_backend.base.extensions.S3Extension;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import com.mipt.team4.cloud_storage_backend.repository.storage.S3ContentRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.S3Wrapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

@ActiveProfiles("test")
@ExtendWith(S3Extension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {S3ContentRepository.class, S3Wrapper.class, S3TestConfig.class})
@EnableConfigurationProperties(StorageConfig.class)
public abstract class BaseS3Test {
  @MockitoBean private NettyServerManager nettyServerManager;

  @TestConfiguration
  static class S3TestConfig {
    @Bean
    public S3Client s3Client(StorageConfig config) {
      StorageConfig.S3 s3Props = config.s3();

      return S3Client.builder()
          .endpointOverride(java.net.URI.create(s3Props.url()))
          .region(software.amazon.awssdk.regions.Region.of(s3Props.region()))
          .credentialsProvider(
              software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                  software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                      s3Props.accessKey(), s3Props.secretKey())))
          .forcePathStyle(true)
          .build();
    }
  }
}
