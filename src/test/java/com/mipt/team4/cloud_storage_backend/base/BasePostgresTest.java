package com.mipt.team4.cloud_storage_backend.base;

import com.mipt.team4.cloud_storage_backend.base.extensions.PostgresExtension;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import com.mipt.team4.cloud_storage_backend.repository.storage.S3ContentRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

@ActiveProfiles("test")
@Transactional
@ExtendWith(PostgresExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public abstract class BasePostgresTest {
  @MockitoBean private NettyServerManager nettyServerManager;
  @MockitoBean private AccessTokenService accessTokenService;
  @MockitoBean private S3Client s3Client;
  @MockitoBean private S3ContentRepository s3ContentRepository;
}
